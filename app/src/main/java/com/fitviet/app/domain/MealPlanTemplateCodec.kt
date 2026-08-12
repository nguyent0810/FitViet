package com.fitviet.app.domain

import org.json.JSONArray
import org.json.JSONObject

/** One meal slot's share of a [com.fitviet.app.data.local.entity.MealPlanTemplateEntity]'s daily
 * calorie target — [slot] matches [com.fitviet.app.data.local.entity.MealEntity.slot]'s existing
 * string convention ("Bữa sáng"/"Bữa trưa"/"Bữa tối"/"Bữa phụ"). */
data class MealPlanTemplateDay(val slot: String, val kcalSharePercent: Int)

/**
 * Encode/decode for [com.fitviet.app.data.local.entity.MealPlanTemplateEntity.dayStructureJson] —
 * same pure, framework-free `org.json` idiom as [ProgramTransfer]/[CustomSplitCodec]. Unlike
 * [CustomSplitCodec] (empty list on failure), [decode] returns `null` on malformed input OR when
 * the shares don't sum to approximately 100% — a template whose day-structure doesn't validate
 * shouldn't silently degrade to "no meals," the caller ([com.fitviet.app.domain.MealPlanGenerator])
 * needs to treat it as unusable, not as a legitimately empty template.
 */
object MealPlanTemplateCodec {
    private const val SHARE_TOLERANCE_PERCENT = 5

    fun encode(days: List<MealPlanTemplateDay>): String {
        val array = JSONArray()
        days.forEach { day ->
            array.put(JSONObject().put("slot", day.slot).put("kcalSharePercent", day.kcalSharePercent))
        }
        return array.toString()
    }

    fun decode(json: String): List<MealPlanTemplateDay>? = try {
        val array = JSONArray(json)
        val days = List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            MealPlanTemplateDay(obj.getString("slot"), obj.getInt("kcalSharePercent"))
        }
        val totalSharePercent = days.sumOf { it.kcalSharePercent }
        if (kotlin.math.abs(totalSharePercent - 100) > SHARE_TOLERANCE_PERCENT) null else days
    } catch (e: Exception) {
        null
    }
}
