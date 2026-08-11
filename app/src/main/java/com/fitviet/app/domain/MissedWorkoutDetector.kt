package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity

/**
 * Detection-only, never a schedule mutation — see the "Hit & Run" plan's adaptive-scheduling
 * section. Run on Dashboard load against the active plan's past days; the repository is
 * responsible for flipping a returned day's [MonthlyPlanDayEntity.status] to MISSED (a marker so
 * re-detection doesn't re-prompt every app foreground) — [effectiveEpochDay] is never touched
 * here or by that flip, only an explicit user choice (push-to-today / manual swap) ever moves a
 * day.
 */
object MissedWorkoutDetector {
    /** [pastNonRestDays] should already be scoped to one plan and filtered to
     * `effectiveEpochDay < today` (see
     * [com.fitviet.app.data.local.dao.MonthlyPlanDayDao.getPastNonRestDays]) — this narrows
     * further to days still [MonthlyPlanDayStatus.SCHEDULED] with no linked
     * [com.fitviet.app.data.local.entity.WorkoutSessionEntity] (i.e. genuinely never dealt with),
     * ignoring ones already MISSED/SKIPPED/RESCHEDULED from a prior detection or user action. */
    fun findMissed(pastNonRestDays: List<MonthlyPlanDayEntity>, completedDayIds: Set<Long>): List<MonthlyPlanDayEntity> =
        pastNonRestDays.filter { it.status == MonthlyPlanDayStatus.SCHEDULED.name && it.id !in completedDayIds }
}
