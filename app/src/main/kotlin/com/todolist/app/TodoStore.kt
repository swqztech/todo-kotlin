package com.todolist.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONException

/**
 * 本地待办存储，键名与 Taro / 小程序相同，便于 JSON 互通。
 */
class TodoStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val items = mutableListOf<TodoItem>()
    private var nextId = 1

    init {
        load()
    }

    private fun recomputeNextId() {
        nextId = 1
        for (item in items) {
            if (item.id >= nextId) {
                nextId = item.id + 1
            }
        }
    }

    fun load() {
        items.clear()
        val raw = prefs.getString(STORAGE_KEY, null)
        if (raw.isNullOrEmpty()) {
            nextId = 1
            return
        }
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                items.add(TodoItem.fromJson(arr.getJSONObject(i)))
            }
            recomputeNextId()
        } catch (_: JSONException) {
            items.clear()
            nextId = 1
        }
    }

    private fun save() {
        try {
            val arr = JSONArray()
            for (item in items) {
                arr.put(item.toJson())
            }
            prefs.edit().putString(STORAGE_KEY, arr.toString(2)).apply()
        } catch (_: JSONException) {
            // 忽略序列化失败
        }
    }

    fun getItems(): List<TodoItem> = items.toList()

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        items.add(TodoItem(nextId++, t, false))
        save()
    }

    private fun addDone(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        items.add(TodoItem(nextId++, t, true))
        save()
    }

    fun setDone(index: Int, done: Boolean) {
        if (index !in items.indices) return
        if (items[index].done == done) return
        items[index].done = done
        save()
    }

    fun removeAt(index: Int) {
        if (index !in items.indices) return
        items.removeAt(index)
        save()
    }

    fun moveItem(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices || from == to) return
        val row = items.removeAt(from)
        items.add(to, row)
        save()
    }

    fun updateText(index: Int, text: String) {
        val t = text.trim()
        if (t.isEmpty() || index !in items.indices) return
        items[index].text = t
        save()
    }

    fun importMarkdown(content: String) {
        for (raw in content.split("\r?\n".toRegex())) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val lower = line.lowercase()
            when {
                lower.startsWith("- [x]") -> addDone(line.substring(5).trim())
                line.startsWith("- [ ]") -> add(line.substring(5).trim())
                else -> add(line)
            }
        }
    }

    fun exportMarkdown(): String = buildString {
        items.forEachIndexed { i, item ->
            if (i > 0) append('\n')
            append(if (item.done) "- [x] " else "- [ ] ")
            append(item.text)
        }
    }

    fun countsLabel(): String {
        var left = 0
        var done = 0
        for (item in items) {
            if (item.done) done++ else left++
        }
        return "还剩 $left · 已完成 $done"
    }

    fun filteredIndices(kind: FilterKind): List<Int> {
        val result = mutableListOf<Int>()
        for (i in items.indices) {
            val item = items[i]
            if (kind == FilterKind.ACTIVE && item.done) continue
            if (kind == FilterKind.DONE && !item.done) continue
            result.add(i)
        }
        return result
    }

    @Throws(JSONException::class)
    fun replaceFromJson(jsonText: String) {
        val arr = JSONArray(jsonText)
        items.clear()
        for (i in 0 until arr.length()) {
            items.add(TodoItem.fromJson(arr.getJSONObject(i)))
        }
        recomputeNextId()
        save()
    }

    companion object {
        private const val PREFS = "todo_store"
        /** 与 todo-taro / todo-miniapp 一致 */
        private const val STORAGE_KEY = "todo_taro_todos"
    }
}
