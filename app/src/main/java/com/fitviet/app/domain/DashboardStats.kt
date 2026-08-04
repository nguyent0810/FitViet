package com.fitviet.app.domain

import java.time.LocalDate

data class DayVolume(val date: LocalDate, val volumeKg: Double)

data class DashboardStats(
    val streakDays: Int,
    val sessionsThisWeek: Int,
    val volumeThisWeekKg: Double,
    val last7Days: List<DayVolume>,
)
