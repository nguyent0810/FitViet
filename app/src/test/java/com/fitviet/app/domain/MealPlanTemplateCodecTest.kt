package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealPlanTemplateCodecTest {

    @Test
    fun `round trip preserves every day's slot and share`() {
        val days = listOf(
            MealPlanTemplateDay("Bữa sáng", 25),
            MealPlanTemplateDay("Bữa trưa", 35),
            MealPlanTemplateDay("Bữa tối", 30),
            MealPlanTemplateDay("Bữa phụ", 10),
        )

        val decoded = MealPlanTemplateCodec.decode(MealPlanTemplateCodec.encode(days))

        assertEquals(days, decoded)
    }

    @Test
    fun `malformed json decodes to null`() {
        assertNull(MealPlanTemplateCodec.decode("not json"))
        assertNull(MealPlanTemplateCodec.decode("{\"not\": \"an array\"}"))
    }

    @Test
    fun `shares not summing to approximately 100 decode to null`() {
        val days = listOf(MealPlanTemplateDay("Bữa sáng", 20), MealPlanTemplateDay("Bữa trưa", 20))

        assertNull(MealPlanTemplateCodec.decode(MealPlanTemplateCodec.encode(days)))
    }

    @Test
    fun `shares within the tolerance band still decode`() {
        // 25+35+30+8 = 98, within the 5-point tolerance of 100.
        val days = listOf(
            MealPlanTemplateDay("Bữa sáng", 25),
            MealPlanTemplateDay("Bữa trưa", 35),
            MealPlanTemplateDay("Bữa tối", 30),
            MealPlanTemplateDay("Bữa phụ", 8),
        )

        assertEquals(days, MealPlanTemplateCodec.decode(MealPlanTemplateCodec.encode(days)))
    }
}
