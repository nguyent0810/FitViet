package com.fitviet.app.domain

import org.json.JSONArray
import org.json.JSONObject

/** Encodes/decodes [CustomSplitDay] lists for
 * [com.fitviet.app.data.local.entity.SplitTemplateEntity.dayDefinitionsJson] — same pure,
 * framework-free `org.json` idiom as [ProgramTransfer], since Custom Split's authoring UI doesn't
 * ship in this pass but the storage format needs to exist and round-trip correctly for the day
 * the UI does land (see the "Hit & Run" plan's scope note). [decode] returns an empty list rather
 * than throwing on malformed input, matching this app's "a corrupt/foreign string degrades to
 * empty, doesn't crash a read" convention elsewhere (e.g. [Converters]'s list converters). */
object CustomSplitCodec {
    fun encode(days: List<CustomSplitDay>): String {
        val array = JSONArray()
        days.forEach { day ->
            val groups = JSONArray()
            day.muscleGroups.forEach { groups.put(it.name) }
            array.put(JSONObject().put("label", day.label).put("muscleGroups", groups))
        }
        return array.toString()
    }

    fun decode(json: String): List<CustomSplitDay> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                val groupsArray = obj.getJSONArray("muscleGroups")
                val groups = List(groupsArray.length()) { j -> MuscleGroup.valueOf(groupsArray.getString(j)) }.toSet()
                CustomSplitDay(obj.getString("label"), groups)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
