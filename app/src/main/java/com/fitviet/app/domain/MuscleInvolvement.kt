package com.fitviet.app.domain

/**
 * Validates [com.fitviet.app.data.local.entity.ExerciseEntity.involvementPercents] (Gate 44,
 * feature #9, mismatch #8). The contract: empty is a valid "hide the involvement card" signal, not
 * an error; otherwise there must be exactly one value per displayed muscle (primary +
 * secondaries), each in `0..100`, summing to exactly 100. "Primary muscle first" is an authoring
 * convention enforced by construction order, not something re-derivable from the percentages alone
 * (there's no way to tell which number belongs to which muscle without the labels) — so it isn't
 * checked here.
 */
object MuscleInvolvement {
    fun isValid(involvementPercents: List<Int>, secondaryMuscleCount: Int): Boolean {
        if (involvementPercents.isEmpty()) return true
        if (involvementPercents.size != secondaryMuscleCount + 1) return false
        if (involvementPercents.any { it !in 0..100 }) return false
        return involvementPercents.sum() == 100
    }
}
