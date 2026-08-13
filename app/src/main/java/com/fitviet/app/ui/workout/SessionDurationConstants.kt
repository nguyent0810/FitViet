package com.fitviet.app.ui.workout

/** Seconds of actual lifting time assumed per rep (concentric + eccentric) when estimating how long
 * a block takes — a transparent estimate, not a promise; the user's real pace, rest add/skip taps,
 * and edited weight/reps all make the real session run long or short. Used by
 * [ProgramDayWorkoutPlanner.estimateDurationMinutes] (day-preview screen) and
 * [com.fitviet.app.domain.MonthlyPlanDayCardEstimator]/[com.fitviet.app.domain.MonthlyPlanGenerator]
 * (Dashboard Today-card / generation-time duration budgeting), which all need the exact same
 * assumptions so their figures can never quietly drift apart. */
internal const val SECONDS_PER_REP = 3

/** Fixed overhead per exercise — walking to the next station, loading the bar, reading the setup —
 * added once per block regardless of set count. */
internal const val TRANSITION_SECONDS = 30
