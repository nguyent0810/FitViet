package com.fitviet.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `kgToLb matches the standard conversion factor`() {
        assertEquals(220.462, kgToLb(100.0), 0.001)
        assertEquals(0.0, kgToLb(0.0), 0.0001)
    }

    @Test
    fun `cmToIn matches the standard conversion factor`() {
        assertEquals(39.3701, cmToIn(100.0), 0.001)
    }

    @Test
    fun `formatWeightUnit stays in kg when metric is selected`() {
        assertEquals("72 kg", formatWeightUnit(72.0, useImperial = false))
    }

    @Test
    fun `formatWeightUnit converts to lb when imperial is selected`() {
        // 72 kg is ~158.7 lb.
        assertEquals("158,7 lb", formatWeightUnit(72.0, useImperial = true))
    }

    @Test
    fun `formatLengthUnit stays in cm when metric is selected`() {
        assertEquals("98 cm", formatLengthUnit(98.0, useImperial = false))
    }

    @Test
    fun `formatLengthUnit converts to inches when imperial is selected`() {
        // 98 cm is ~38.6 in.
        assertEquals("38,6 in", formatLengthUnit(98.0, useImperial = true))
    }
}
