package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [MonthlyPlanTodayResolver.classify] — the "Hit & Run" redesign (Gate 1c) pure function
 * [com.fitviet.app.data.repository.MonthlyPlanRepository.observeTodaySession] builds its
 * [TodayMonthlyPlanCard] resolution on top of. Flagged as untested in the Phase 1 checkpoint
 * review (its own KDoc advertises testability, but nothing exercised it) — this file closes that
 * gap. Also covers [MonthlyPlanTodayResolver.resolve]'s reschedule-priority tie-break directly,
 * since [MonthlyPlanTodayResolver.classify] is a thin wrapper around it.
 */
class MonthlyPlanTodayResolverTest {

    private val today = LocalDate.of(2024, 1, 3)

    private fun day(
        id: Long,
        effectiveEpochDay: Long = today.toEpochDay(),
        isRestDay: Boolean = false,
        sessionType: String? = "Push",
        status: MonthlyPlanDayStatus = MonthlyPlanDayStatus.SCHEDULED,
    ) = MonthlyPlanDayEntity(
        id = id,
        monthlyPlanWeekId = 1L,
        plannedEpochDay = effectiveEpochDay,
        effectiveEpochDay = effectiveEpochDay,
        dayOfWeek = today.dayOfWeek.value,
        isRestDay = isRestDay,
        sessionType = sessionType,
        muscleGroupCodes = emptyList(),
        status = status.name,
    )

    @Test
    fun `no active plan id classifies as NoPlan regardless of the day list`() {
        val result = MonthlyPlanTodayResolver.classify(
            activePlanId = null,
            days = listOf(day(id = 1L)),
            today = today,
        )

        assertEquals(TodayPlanDayState.NoPlan, result)
    }

    @Test
    fun `an active plan with no row for today classifies as PlanFinished`() {
        val result = MonthlyPlanTodayResolver.classify(
            activePlanId = 42L,
            days = listOf(day(id = 1L, effectiveEpochDay = today.plusDays(-1).toEpochDay())),
            today = today,
        )

        assertEquals(TodayPlanDayState.PlanFinished, result)
    }

    @Test
    fun `a rest day classifies as RestDay`() {
        val result = MonthlyPlanTodayResolver.classify(
            activePlanId = 42L,
            days = listOf(day(id = 1L, isRestDay = true, sessionType = null)),
            today = today,
        )

        assertEquals(TodayPlanDayState.RestDay, result)
    }

    @Test
    fun `a non-rest row with no sessionType also classifies as RestDay rather than crashing`() {
        // Defensive case: isRestDay=false but sessionType=null shouldn't happen for a
        // generator-produced row, but classify() treats it the same as a real rest day rather
        // than resolving to a HasSession with a null label.
        val result = MonthlyPlanTodayResolver.classify(
            activePlanId = 42L,
            days = listOf(day(id = 1L, isRestDay = false, sessionType = null)),
            today = today,
        )

        assertEquals(TodayPlanDayState.RestDay, result)
    }

    @Test
    fun `a training day classifies as HasSession carrying its day id and session type`() {
        val result = MonthlyPlanTodayResolver.classify(
            activePlanId = 42L,
            days = listOf(day(id = 7L, sessionType = "Pull")),
            today = today,
        )

        assertEquals(TodayPlanDayState.HasSession(dayId = 7L, sessionType = "Pull"), result)
    }

    @Test
    fun `resolve prefers a RESCHEDULED non-rest row over the day originally occupying today`() {
        // pushMissedDayToToday leaves the original day in place (see the class doc's "no cascade"
        // note) — a collision like this is a valid, expected state, not a data bug.
        val original = day(id = 1L, sessionType = "Push")
        val rescheduled = day(id = 2L, sessionType = "Legs", status = MonthlyPlanDayStatus.RESCHEDULED)

        val resolved = MonthlyPlanTodayResolver.resolve(listOf(original, rescheduled), today)

        assertEquals(2L, resolved?.id)
    }

    @Test
    fun `resolve prefers any non-rest row over a rest row when both land on today`() {
        val restDay = day(id = 1L, isRestDay = true, sessionType = null)
        val trainingDay = day(id = 2L, sessionType = "Push")

        val resolved = MonthlyPlanTodayResolver.resolve(listOf(restDay, trainingDay), today)

        assertEquals(2L, resolved?.id)
    }

    @Test
    fun `resolve returns null when nothing matches today's epoch day`() {
        val resolved = MonthlyPlanTodayResolver.resolve(
            listOf(day(id = 1L, effectiveEpochDay = today.plusDays(1).toEpochDay())),
            today,
        )

        assertNull(resolved)
    }
}
