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

    /** Same JSON-array approach as [fromStringList]/[toStringList] — used by
     * [com.fitviet.app.data.local.entity.ReminderEntity.daysOfWeek] (Gate 38) instead of a bespoke
     * bitmask/CSV encoding. Distinguished from the `List<String>` pair above by Room's compile-time
     * type resolution (exact declared generic type, not JVM-erased signature), so both converter
     * pairs coexist safely despite `List<Int>`/`List<String>` erasing identically at the JVM level. */
    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        val array = JSONArray()
        value.forEach(array::put)
        return array.toString()
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        if (value.isEmpty()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { array.getInt(it) }
    }

    /** Same JSON-array approach as the pairs above — added for
     * [com.fitviet.app.data.local.entity.MonthlyPlanEntity.exclusionExerciseIds] ("Hit & Run",
     * Gate 63+), which needs real exercise ids (Long), not the String/Int lists already covered. */
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        val array = JSONArray()
        value.forEach(array::put)
        return array.toString()
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        if (value.isEmpty()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { array.getLong(it) }
    }
}
