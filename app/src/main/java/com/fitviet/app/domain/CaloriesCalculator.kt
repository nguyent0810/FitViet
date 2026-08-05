package com.fitviet.app.domain

import kotlin.math.roundToInt

/**
 * Feature #10 — a transparent, rough calories-burned estimate for a completed session. "Only ever
 * an estimate," per the roadmap note, not a promise: this app has no reliable bodyweight signal
 * wired into the workout flow (the profile's logged bodyweight lives in a separate repository the
 * in-progress workout screen doesn't depend on), so — like most fitness apps that don't ask for
 * your weight up front — it assumes a fixed average adult bodyweight. Uses the standard MET formula
 * (kcal/min = MET × 3.5 × bodyweightKg ÷ 200) with a moderate-effort resistance-training MET value
 * from the Compendium of Physical Activities.
 */
object CaloriesCalculator {
    private const val ASSUMED_BODYWEIGHT_KG = 70.0
    private const val RESISTANCE_TRAINING_MET = 5.0
    private const val KCAL_PER_MINUTE = RESISTANCE_TRAINING_MET * 3.5 * ASSUMED_BODYWEIGHT_KG / 200.0

    fun estimateKcal(durationSeconds: Int): Int {
        val minutes = durationSeconds.coerceAtLeast(0) / 60.0
        return (minutes * KCAL_PER_MINUTE).roundToInt()
    }
}
