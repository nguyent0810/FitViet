package com.fitviet.app.domain

import org.json.JSONArray

/**
 * JSON encode/decode for [com.fitviet.app.data.local.entity.RecipeEntity.instructionsJson] — a
 * plain `List<String>` of numbered steps. Pure and framework-free, same `decode`-returns-`null`-
 * on-malformed-input shape as [ProgramTransfer], even though this data never crosses a real trust
 * boundary today (seed-authored only) — keeping the same defensive shape means a future
 * user-authored-recipe feature isn't a breaking contract change.
 */
object RecipeInstructionsCodec {
    fun encode(steps: List<String>): String {
        val array = JSONArray()
        steps.forEach(array::put)
        return array.toString()
    }

    fun decode(json: String): List<String>? = runCatching {
        val array = JSONArray(json)
        List(array.length()) { array.getString(it) }
    }.getOrNull()
}
