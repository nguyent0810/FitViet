package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTargetsCalculatorTest {

    @Test
    fun `cut kcal target uses the cut multiplier`() {
        assertEquals(1820, NutritionTargetsCalculator.defaultKcalTarget(70.0, NutritionGoal.CUT))
    }

    @Test
    fun `maintain kcal target uses the maintain multiplier`() {
        assertEquals(2100, NutritionTargetsCalculator.defaultKcalTarget(70.0, NutritionGoal.MAINTAIN))
    }

    @Test
    fun `bulk kcal target uses the bulk multiplier`() {
        assertEquals(2380, NutritionTargetsCalculator.defaultKcalTarget(70.0, NutritionGoal.BULK))
    }

    @Test
    fun `protein target is a flat 2g per kg regardless of goal`() {
        assertEquals(140, NutritionTargetsCalculator.defaultProteinTargetG(70.0))
        assertEquals(120, NutritionTargetsCalculator.defaultProteinTargetG(60.0))
    }

    @Test
    fun `stored goal name parses to the matching enum value`() {
        assertEquals(NutritionGoal.BULK, NutritionGoal.fromStored("BULK"))
        assertEquals(NutritionGoal.CUT, NutritionGoal.fromStored("CUT"))
        assertEquals(NutritionGoal.MAINTAIN, NutritionGoal.fromStored("MAINTAIN"))
    }

    @Test
    fun `unrecognized or missing stored value defaults to maintain`() {
        assertEquals(NutritionGoal.MAINTAIN, NutritionGoal.fromStored(null))
        assertEquals(NutritionGoal.MAINTAIN, NutritionGoal.fromStored(""))
        assertEquals(NutritionGoal.MAINTAIN, NutritionGoal.fromStored("STRENGTH"))
    }
}
