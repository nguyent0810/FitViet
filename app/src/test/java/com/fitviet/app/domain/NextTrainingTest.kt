package com.fitviet.app.domain

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextTrainingCalculatorTest {

    private fun day(dayOfWeek: DayOfWeek, isRestDay: Boolean = false, titleVi: String = dayOfWeek.name) =
        ProgramScheduleDay(dayOfWeek = dayOfWeek, titleVi = titleVi, isRestDay = isRestDay, exercises = emptyList())

    @Test
    fun `empty schedule has no next training`() {
        assertNull(NextTrainingCalculator.findNext(emptyList(), today = DayOfWeek.MONDAY))
    }

    @Test
    fun `a schedule of only rest days has no next training`() {
        val schedule = DayOfWeek.entries.map { day(it, isRestDay = true) }

        assertNull(NextTrainingCalculator.findNext(schedule, today = DayOfWeek.WEDNESDAY))
    }

    @Test
    fun `today is a training day returns today, marked isToday`() {
        val schedule = listOf(day(DayOfWeek.WEDNESDAY, titleVi = "Ngực & Vai"), day(DayOfWeek.FRIDAY))

        val result = NextTrainingCalculator.findNext(schedule, today = DayOfWeek.WEDNESDAY)

        assertEquals(DayOfWeek.WEDNESDAY, result?.day?.dayOfWeek)
        assertEquals("Ngực & Vai", result?.day?.titleVi)
        assertEquals(true, result?.isToday)
    }

    @Test
    fun `today is a rest day finds the next training day later this week`() {
        val schedule = listOf(
            day(DayOfWeek.MONDAY),
            day(DayOfWeek.TUESDAY, isRestDay = true),
            day(DayOfWeek.WEDNESDAY, isRestDay = true),
            day(DayOfWeek.THURSDAY),
        )

        val result = NextTrainingCalculator.findNext(schedule, today = DayOfWeek.TUESDAY)

        assertEquals(DayOfWeek.THURSDAY, result?.day?.dayOfWeek)
        assertEquals(false, result?.isToday)
    }

    @Test
    fun `wraps into next week when nothing training-worthy remains this week`() {
        // Only Monday trains; asking from Saturday should wrap around to next Monday, not return null.
        val schedule = listOf(
            day(DayOfWeek.MONDAY),
            day(DayOfWeek.TUESDAY, isRestDay = true),
            day(DayOfWeek.WEDNESDAY, isRestDay = true),
            day(DayOfWeek.THURSDAY, isRestDay = true),
            day(DayOfWeek.FRIDAY, isRestDay = true),
            day(DayOfWeek.SATURDAY, isRestDay = true),
            day(DayOfWeek.SUNDAY, isRestDay = true),
        )

        val result = NextTrainingCalculator.findNext(schedule, today = DayOfWeek.SATURDAY)

        assertEquals(DayOfWeek.MONDAY, result?.day?.dayOfWeek)
        assertEquals(false, result?.isToday)
    }

    @Test
    fun `picks the nearest upcoming training day, not just any training day`() {
        val schedule = listOf(day(DayOfWeek.MONDAY), day(DayOfWeek.FRIDAY))

        val result = NextTrainingCalculator.findNext(schedule, today = DayOfWeek.TUESDAY)

        assertEquals(DayOfWeek.FRIDAY, result?.day?.dayOfWeek)
    }
}

class ProgramProgressTest {

    @Test
    fun `fraction is the completed share of the weekly target`() {
        assertEquals(0.5f, ProgramProgress(completedThisWeek = 2, targetPerWeek = 4).fraction, 0.0001f)
    }

    @Test
    fun `fraction caps at 1 when the target is exceeded`() {
        assertEquals(1f, ProgramProgress(completedThisWeek = 6, targetPerWeek = 4).fraction, 0.0001f)
    }

    @Test
    fun `fraction is 0 for a zero weekly target, not a division error`() {
        assertEquals(0f, ProgramProgress(completedThisWeek = 3, targetPerWeek = 0).fraction, 0.0001f)
    }

    @Test
    fun `fraction is 0 when nothing has been completed yet`() {
        assertEquals(0f, ProgramProgress(completedThisWeek = 0, targetPerWeek = 4).fraction, 0.0001f)
    }
}
