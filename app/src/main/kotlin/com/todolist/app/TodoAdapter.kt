package com.todolist.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** 展示行：storeIndex + 展示用字段 */
class TodoAdapter : RecyclerView.Adapter<TodoAdapter.Holder>() {

    data class Row(
        val storeIndex: Int,
        val text: String,
        val done: Boolean,
    )

    interface Listener {
        fun onRowClick(listPosition: Int)
        fun onRowLongClick(listPosition: Int)
        fun onToggleDone(listPosition: Int)
    }

    private val rows = mutableListOf<Row>()
    private var listener: Listener? = null
    private var selectedListIndex = -1

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun submit(next: List<Row>, selectedListIndex: Int) {
        rows.clear()
        rows.addAll(next)
        this.selectedListIndex = selectedListIndex
        notifyDataSetChanged()
    }

    fun getRow(listPosition: Int): Row? =
        if (listPosition in rows.indices) rows[listPosition] else null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.text.text = row.text
        holder.text.paintFlags = if (row.done) {
            holder.text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        holder.check.text = if (row.done) "✓" else ""
        holder.check.isSelected = row.done
        holder.itemView.isSelected = position == selectedListIndex

        holder.itemView.setOnClickListener {
            listener?.onRowClick(holder.bindingAdapterPosition)
        }
        holder.itemView.setOnLongClickListener {
            listener?.onRowLongClick(holder.bindingAdapterPosition)
            true
        }
        holder.check.setOnClickListener {
            listener?.onToggleDone(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = rows.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val check: TextView = itemView.findViewById(R.id.check)
        val text: TextView = itemView.findViewById(R.id.row_text)
    }
}
