package com.fitviet.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

/** Stores String lists (tags, muscle groups, instructions) as a JSON array column. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val array = JSONArray()
        value.forEach(array::put)
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}
