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

    /** Redesign Gate 2b — reuses [levelSteps]'s exact same literals so tapping a sample program to
     * generate a monthly plan (see `ProgramsViewModel.generateFromProgram`) picks a real
     * [ExerciseDifficulty] instead of always hardcoding [ExerciseDifficulty.BEGINNER]; `null`
     * (unrated/unrecognized, same cases [levelSteps] returns null for) also falls back to
     * `BEGINNER` — the safest default, matching onboarding's own precedent of defaulting there. */
    fun exerciseDifficultyFor(level: String): ExerciseDifficulty = when (levelSteps(level)) {
        1 -> ExerciseDifficulty.BEGINNER
        2 -> ExerciseDifficulty.INTERMEDIATE
        3 -> ExerciseDifficulty.ADVANCED
        else -> ExerciseDifficulty.BEGINNER
    }
}
