package com.fitviet.app.data.local.seed

import com.fitviet.app.domain.MuscleInvolvement
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feature #9 (Gate 44) — the actual safety net for the "author all 155 exercises correctly" gate:
 * every one of [SeedData.exercises]' real, hand-authored `involvementPercents` lists must satisfy
 * [MuscleInvolvement]'s contract. Authoring 155 individual assertions by hand isn't the point —
 * this iterates the real seed data so a future edit to any single exercise (or a new one added
 * without involvement data considered) fails loudly here instead of silently shipping a broken bar.
 */
class SeedDataMuscleInvolvementTest {

    @Test
    fun `every seeded exercise's involvementPercents is either empty or a valid distribution`() {
        val invalid = SeedData.exercises.filter { exercise ->
            !MuscleInvolvement.isValid(exercise.involvementPercents, exercise.secondaryMuscles.size)
        }
        assertTrue(
            "Exercises with invalid involvementPercents: ${invalid.map { it.nameEn }}",
            invalid.isEmpty(),
        )
    }

    @Test
    fun `at least some exercises are non-empty (the feature actually authors data, not all-hidden)`() {
        val nonEmptyCount = SeedData.exercises.count { it.involvementPercents.isNotEmpty() }
        assertTrue("Expected most exercises to have authored involvement data", nonEmptyCount > 100)
    }

    @Test
    fun `cardio, stretching, and functional exercises are the ones left empty by design`() {
        val emptyOnes = SeedData.exercises.filter { it.involvementPercents.isEmpty() }
        val unexpectedlyEmpty = emptyOnes.filterNot {
            it.muscleGroupCode in setOf("CARDIO", "STRETCHING", "FUNCTIONAL")
        }
        assertTrue(
            "Exercises hidden from the involvement card outside the documented exclusion groups: " +
                unexpectedlyEmpty.map { it.nameEn },
            unexpectedlyEmpty.isEmpty(),
        )
    }
}
