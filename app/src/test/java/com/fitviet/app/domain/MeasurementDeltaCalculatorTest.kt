package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.MeasurementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementDeltaCalculatorTest {

    private fun measurement(weight: Double? = null, chest: Double? = null, waist: Double? = null, arm: Double? = null) =
        MeasurementEntity(epochDay = 0, weightKg = weight, chestCm = chest, waistCm = waist, armCm = arm)

    @Test
    fun `no previous measurement yields all-null deltas`() {
        val deltas = MeasurementDeltaCalculator.compute(measurement(weight = 72.0), previous = null)

        assertNull(deltas.weightKg)
        assertNull(deltas.chestCm)
        assertNull(deltas.waistCm)
        assertNull(deltas.armCm)
    }

    @Test
    fun `no latest measurement yields all-null deltas`() {
        val deltas = MeasurementDeltaCalculator.compute(latest = null, previous = measurement(weight = 70.0))

        assertNull(deltas.weightKg)
    }

    @Test
    fun `delta is latest minus previous, matching the seed data deltas`() {
        val previous = measurement(weight = 70.8, chest = 96.0, waist = 81.0, arm = 35.5)
        val latest = measurement(weight = 72.0, chest = 98.0, waist = 80.0, arm = 36.0)

        val deltas = MeasurementDeltaCalculator.compute(latest, previous)

        assertEquals(1.2, deltas.weightKg!!, 0.0001)
        assertEquals(2.0, deltas.chestCm!!, 0.0001)
        assertEquals(-1.0, deltas.waistCm!!, 0.0001)
        assertEquals(0.5, deltas.armCm!!, 0.0001)
    }

    @Test
    fun `a metric missing from either check-in has a null delta without affecting the others`() {
        val previous = measurement(weight = 70.0, chest = null)
        val latest = measurement(weight = 71.0, chest = 98.0)

        val deltas = MeasurementDeltaCalculator.compute(latest, previous)

        assertEquals(1.0, deltas.weightKg!!, 0.0001)
        assertNull(deltas.chestCm)
    }
}
