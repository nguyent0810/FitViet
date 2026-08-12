package com.fitviet.app.domain

import java.time.LocalDate

/** Resolves "today" against a [com.fitviet.app.data.local.entity.UserMealPlanEntity]'s repeating
 * week — mirrors [MonthlyPlanTodayResolver]'s job for the workout-plan feature, but simpler since
 * meal plans have no real-calendar-date model (see [com.fitviet.app.data.local.entity.MealPlanDayEntity]'s
 * doc comment): [java.time.DayOfWeek.getValue] already returns the same 1=Monday..7=Sunday ISO
 * convention [com.fitviet.app.data.local.entity.MealPlanDayEntity.dayOfWeek] stores, so no lookup
 * table is needed, just this single pure function kept in one place rather than inlined at every
 * call site.
 */
object MealPlanTodayResolver {
    fun todayDayOfWeek(today: LocalDate = LocalDate.now()): Int = today.dayOfWeek.value
}
