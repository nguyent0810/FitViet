package com.fitviet.app.domain

/**
 * Maps [com.fitviet.app.data.local.entity.ProgramEntity.level] to a 1-3 difficulty-bar count for
 * the Programs list badge (Gate 42, feature #8). All 3 tiers are mapped even though no seeded
 * program currently uses "Nâng cao" (mismatch #1 in the gate plan) — an imported program can carry
 * any string, including a future "Nâng cao". `null` covers both "Mọi trình độ" (a program that
 * genuinely isn't tied to one level) and any unrecognized/imported string — both render as all
 * bars muted, not an error.
 */
object ProgramDifficulty {
    fun levelSteps(level: String): Int? = when (level) {
        "Mới bắt đầu" -> 1
        "Trung cấp" -> 2
        "Nâng cao" -> 3
        else -> null
    }
}
