package com.todolist.app

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var store: TodoStore
    private var filter = FilterKind.ALL
    private var selectedListIndex = -1

    private lateinit var input: TextInputEditText
    private lateinit var dateText: TextView
    private lateinit var countsText: TextView
    private lateinit var emptyText: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: TodoAdapter

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = TodoStore(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)

        input = findViewById(R.id.input)
        dateText = findViewById(R.id.date_text)
        countsText = findViewById(R.id.counts_text)
        emptyText = findViewById(R.id.empty_text)
        list = findViewById(R.id.todo_list)

        dateText.text = todayLabel()

        adapter = TodoAdapter()
        adapter.setListener(object : TodoAdapter.Listener {
            override fun onRowClick(listPosition: Int) {
                selectedListIndex = listPosition
                refreshList()
            }

            override fun onRowLongClick(listPosition: Int) {
                adapter.getRow(listPosition)?.let { showEditDialog(it) }
            }

            override fun onToggleDone(listPosition: Int) {
                val row = adapter.getRow(listPosition) ?: return
                store.setDone(row.storeIndex, !row.done)
                refreshList()
            }
        })
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        setupFilterChips()

        findViewById<View>(R.id.btn_add).setOnClickListener { onAdd() }
        findViewById<View>(R.id.btn_export).setOnClickListener { onExport() }
        findViewById<View>(R.id.btn_import).setOnClickListener { onImport() }
        findViewById<View>(R.id.btn_paste).setOnClickListener { onPaste() }
        findViewById<View>(R.id.btn_move_up).setOnClickListener { onMove(-1) }
        findViewById<View>(R.id.btn_move_down).setOnClickListener { onMove(1) }
        findViewById<View>(R.id.btn_delete).setOnClickListener { onDelete() }

        refreshList()
    }

    private fun setupFilterChips() {
        val ids = intArrayOf(R.id.chip_all, R.id.chip_active, R.id.chip_done)
        val kinds = arrayOf(FilterKind.ALL, FilterKind.ACTIVE, FilterKind.DONE)
        for (i in ids.indices) {
            val chip = findViewById<Chip>(ids[i])
            val kind = kinds[i]
            chip.setOnClickListener {
                filter = kind
                selectedListIndex = -1
                updateFilterChipStyles()
                refreshList()
            }
        }
        updateFilterChipStyles()
    }

    private fun updateFilterChipStyles() {
        setChipChecked(R.id.chip_all, filter == FilterKind.ALL)
        setChipChecked(R.id.chip_active, filter == FilterKind.ACTIVE)
        setChipChecked(R.id.chip_done, filter == FilterKind.DONE)
    }

    private fun setChipChecked(id: Int, checked: Boolean) {
        findViewById<Chip>(id).isChecked = checked
    }

    private fun onAdd() {
        val text = input.text?.toString() ?: return
        if (text.trim().isEmpty()) return
        store.add(text)
        input.setText("")
        selectedListIndex = -1
        refreshList()
    }

    private fun onExport() {
        val md = store.exportMarkdown()
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("todo", md))
        Toast.makeText(this, R.string.export_ok, Toast.LENGTH_SHORT).show()
    }

    private fun onPaste() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip
        if (cm == null || !cm.hasPrimaryClip() || clip == null || clip.itemCount == 0) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val data = clip.getItemAt(0).text
        if (data == null || data.toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
            return
        }
        store.importMarkdown(data.toString())
        selectedListIndex = -1
        refreshList()
        Toast.makeText(this, R.string.import_ok, Toast.LENGTH_SHORT).show()
    }

    private fun onImport() {
        pickFile.launch(arrayOf("text/*", "application/json", "*/*"))
    }

    private fun importFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inStream ->
                val content = BufferedReader(InputStreamReader(inStream, StandardCharsets.UTF_8))
                    .readText()
                    .trim()
                val name = uri.lastPathSegment?.lowercase(Locale.ROOT) ?: ""
                if (name.endsWith(".json") || content.trimStart().startsWith("[")) {
                    store.replaceFromJson(content)
                } else {
                    store.importMarkdown(content)
                }
                selectedListIndex = -1
                refreshList()
                Toast.makeText(this, R.string.import_ok, Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.import_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onDelete() {
        val row = selectedRow()
        if (row == null) {
            Toast.makeText(this, R.string.select_first, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                store.removeAt(row.storeIndex)
                selectedListIndex = -1
                refreshList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onMove(delta: Int) {
        val row = selectedRow()
        if (row == null) {
            Toast.makeText(this, R.string.select_first, Toast.LENGTH_SHORT).show()
            return
        }
        val newIdx = row.storeIndex + delta
        if (newIdx !in store.getItems().indices) return
        store.moveItem(row.storeIndex, newIdx)
        val indices = store.filteredIndices(filter)
        selectedListIndex = indices.indexOf(newIdx)
        refreshList()
    }

    private fun showEditDialog(row: TodoAdapter.Row) {
        val edit = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(row.text)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_title)
            .setView(edit)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                store.updateText(row.storeIndex, edit.text.toString())
                refreshList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun selectedRow(): TodoAdapter.Row? =
        if (selectedListIndex < 0) null else adapter.getRow(selectedListIndex)

    private fun refreshList() {
        val indices = store.filteredIndices(filter)
        val rows = indices.map { idx ->
            val item = store.getItems()[idx]
            TodoAdapter.Row(idx, item.text, item.done)
        }
        if (selectedListIndex >= rows.size) {
            selectedListIndex = -1
        }
        adapter.submit(rows, selectedListIndex)
        countsText.text = store.countsLabel()
        val empty = rows.isEmpty()
        emptyText.visibility = if (empty) View.VISIBLE else View.GONE
        list.visibility = if (empty) View.GONE else View.VISIBLE
    }

    companion object {
        private fun todayLabel(): String {
            val cal = Calendar.getInstance()
            val weekdays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val wd = cal.get(Calendar.DAY_OF_WEEK) - 1
            return "${month}月${day}日 ${weekdays[wd]}"
        }
    }
}
