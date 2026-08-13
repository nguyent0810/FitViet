package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [MonthlyPlanProgress.dayOfPlan] — the Today card's "NGÀY N/Y" label numerator, a
 * row-count position rather than date arithmetic (see the function's own doc for why).
 */
class MonthlyPlanProgressTest {

    private fun day(id: Long, effectiveEpochDay: Long = 0L) = MonthlyPlanDayEntity(
        id = id,
        monthlyPlanWeekId = 1L,
        plannedEpochDay = effectiveEpochDay,
        effectiveEpochDay = effectiveEpochDay,
        dayOfWeek = 1,
        isRestDay = false,
        sessionType = "Push",
        muscleGroupCodes = emptyList(),
        status = MonthlyPlanDayStatus.SCHEDULED.name,
    )

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
}
