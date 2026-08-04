package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MeasurementEntity

/** Per-metric change vs. the prior check-in — null where either side has no reading for that metric. */
data class MeasurementDeltas(
    val weightKg: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val armCm: Double?,
)

object MeasurementDeltaCalculator {
    fun compute(latest: MeasurementEntity?, previous: MeasurementEntity?): MeasurementDeltas {
        if (latest == null || previous == null) return MeasurementDeltas(null, null, null, null)
        return MeasurementDeltas(
            weightKg = delta(latest.weightKg, previous.weightKg),
            chestCm = delta(latest.chestCm, previous.chestCm),
            waistCm = delta(latest.waistCm, previous.waistCm),
            armCm = delta(latest.armCm, previous.armCm),
        )
    }

    private fun delta(current: Double?, prior: Double?): Double? =
        if (current != null && prior != null) current - prior else null
}
