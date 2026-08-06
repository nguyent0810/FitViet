package com.fitviet.app.domain

import java.time.LocalDate

/** Pure, unit-testable — same rationale as [DashboardStatsCalculator]. The actual weekly-bucketing
 * logic lives in [WeeklyBucketing] (Gate 43), shared with [DashboardStatsCalculator.rangeSeries]. */
object DiaryStatsCalculator {

    fun lastNWeeks(sessions: List<CompletedSession>, today: LocalDate, weeks: Int = 4): List<WeekVolume> =
        WeeklyBucketing.lastNWeeks(sessions, today, weeks)
}
