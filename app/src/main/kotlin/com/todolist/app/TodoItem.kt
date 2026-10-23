package com.todolist.app

import org.json.JSONObject

/** 与 Taro / Flutter 导出 JSON 字段一致 */
data class TodoItem(
    var id: Int,
    var text: String,
    var done: Boolean,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("Id", id)
        put("Text", text)
        put("Done", done)
    }

    companion object {
        fun fromJson(o: JSONObject): TodoItem = TodoItem(
            o.optInt("Id", 0),
            o.optString("Text", ""),
            o.optBoolean("Done", false),
        )
    }
}
