package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [MonthlyPlanProgress.dayOfPlan] — the Today card's "NGÀY N/Y" label numerator, a
 * row-count position rather than date arithmetic (see the function's own doc for why) — and
 * [MonthlyPlanProgress.summarize] — the Kế hoạch tab's progress card.
 */
class MonthlyPlanProgressTest {

    private val today = LocalDate.of(2024, 1, 10)

    private fun day(
        id: Long,
        weekId: Long = 1L,
        effectiveEpochDay: Long = 0L,
        isRestDay: Boolean = false,
        sessionType: String? = "Push",
    ) = MonthlyPlanDayEntity(
        id = id,
        monthlyPlanWeekId = weekId,
        plannedEpochDay = effectiveEpochDay,
        effectiveEpochDay = effectiveEpochDay,
        dayOfWeek = 1,
        isRestDay = isRestDay,
        sessionType = sessionType,
        muscleGroupCodes = emptyList(),
        status = MonthlyPlanDayStatus.SCHEDULED.name,
    )

    private fun week(id: Long, weekIndex: Int) =
        MonthlyPlanWeekEntity(id = id, monthlyPlanId = 1L, weekIndex = weekIndex, phase = PlanPhase.BASE.name)

    @Test
    fun `the first row in the list is day 1`() {
        val days = listOf(day(id = 10L), day(id = 11L), day(id = 12L))

        assertEquals(1, MonthlyPlanProgress.dayOfPlan(days, todayDayId = 10L))
    }

    @Test
    fun `a later row reflects its 1-based position, not its id`() {
        val days = listOf(day(id = 100L), day(id = 200L), day(id = 300L))

        assertEquals(3, MonthlyPlanProgress.dayOfPlan(days, todayDayId = 300L))
    }

    @Test
    fun `an id absent from the list returns null rather than a bogus index`() {
        val days = listOf(day(id = 1L), day(id = 2L))

        assertNull(MonthlyPlanProgress.dayOfPlan(days, todayDayId = 999L))
    }

    @Test
    fun `summarize counts only non-rest days as sessions`() {
        val days = listOf(
            day(id = 1L, isRestDay = false),
            day(id = 2L, isRestDay = true, sessionType = null),
            day(id = 3L, isRestDay = false),
        )

        val summary = MonthlyPlanProgress.summarize(listOf(week(1L, 0)), days, completedDayIds = setOf(1L), today = today)

        assertEquals(1, summary.completedSessions)
        assertEquals(2, summary.totalSessions)
    }

    @Test
    fun `summarize resolves the current week from today's own row`() {
        val weeks = listOf(week(id = 1L, weekIndex = 0), week(id = 2L, weekIndex = 1))
        val days = listOf(
            day(id = 1L, weekId = 1L, effectiveEpochDay = today.minusDays(1).toEpochDay()),
            day(id = 2L, weekId = 2L, effectiveEpochDay = today.toEpochDay()),
        )

        val summary = MonthlyPlanProgress.summarize(weeks, days, completedDayIds = emptySet(), today = today)

        assertEquals(2, summary.currentWeekNumber) // weekIndex 1 -> 1-based week 2
    }

    @Test
    fun `summarize falls back to the last week when today has no row`() {
        val weeks = listOf(week(id = 1L, weekIndex = 0), week(id = 2L, weekIndex = 1))
        val days = listOf(day(id = 1L, weekId = 1L, effectiveEpochDay = today.minusDays(30).toEpochDay()))

        val summary = MonthlyPlanProgress.summarize(weeks, days, completedDayIds = emptySet(), today = today)

        assertEquals(2, summary.currentWeekNumber)
    }

    @Test
    fun `summarize returns a null week number for a plan with no weeks`() {
        val summary = MonthlyPlanProgress.summarize(emptyList(), emptyList(), completedDayIds = emptySet(), today = today)

        assertNull(summary.currentWeekNumber)
    }
}
