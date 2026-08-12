package com.fitviet.app.util

import java.text.Normalizer

/** Lowercase, diacritic-stripped form of a Vietnamese string, for accent-insensitive search/sort
 * (e.g. [com.fitviet.app.data.local.entity.FoodEntity.normalizedName]) — "Ức gà" -> "uc ga". */
fun normalizeVietnamese(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
    val withoutDiacritics = decomposed.replace(Regex("\\p{Mn}+"), "")
    return withoutDiacritics
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase()
        .trim()
}
