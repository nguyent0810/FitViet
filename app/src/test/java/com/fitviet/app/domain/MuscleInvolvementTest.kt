package com.fitviet.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleInvolvementTest {

    @Test
    fun `an empty list is valid regardless of secondary muscle count - the explicit hide-card signal`() {
        assertTrue(MuscleInvolvement.isValid(emptyList(), secondaryMuscleCount = 3))
    }

    @Test
    fun `a single 100 with no secondary muscles is valid`() {
        assertTrue(MuscleInvolvement.isValid(listOf(100), secondaryMuscleCount = 0))
    }

    @Test
    fun `values summing to exactly 100 with the right count are valid`() {
        assertTrue(MuscleInvolvement.isValid(listOf(55, 23, 22), secondaryMuscleCount = 2))
    }

    @Test
    fun `a count mismatch (too few or too many values) is invalid`() {
        assertFalse(MuscleInvolvement.isValid(listOf(55, 45), secondaryMuscleCount = 2))
        assertFalse(MuscleInvolvement.isValid(listOf(55, 23, 22, 0), secondaryMuscleCount = 2))
    }

    @Test
    fun `values not summing to 100 are invalid`() {
        assertFalse(MuscleInvolvement.isValid(listOf(50, 40), secondaryMuscleCount = 1))
        assertFalse(MuscleInvolvement.isValid(listOf(60, 50), secondaryMuscleCount = 1))
    }

    @Test
    fun `a value outside 0 to 100 is invalid even if the list still sums to 100`() {
        assertFalse(MuscleInvolvement.isValid(listOf(150, -50), secondaryMuscleCount = 1))
    }

    @Test
    fun `a zero-percent muscle is allowed as long as the total is still 100`() {
        assertTrue(MuscleInvolvement.isValid(listOf(100, 0), secondaryMuscleCount = 1))
    }
}
