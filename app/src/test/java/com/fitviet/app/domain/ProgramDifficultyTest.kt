package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramDifficultyTest {

    @Test
    fun `beginner level maps to 1 step`() {
        assertEquals(1, ProgramDifficulty.levelSteps("Mới bắt đầu"))
    }

    @Test
    fun `intermediate level maps to 2 steps`() {
        assertEquals(2, ProgramDifficulty.levelSteps("Trung cấp"))
    }

    @Test
    fun `advanced level maps to 3 steps even though no seeded program uses it yet`() {
        assertEquals(3, ProgramDifficulty.levelSteps("Nâng cao"))
    }

    @Test
    fun `Moi trinh do (no specific level) maps to null, not a step count`() {
        assertNull(ProgramDifficulty.levelSteps("Mọi trình độ"))
    }

    @Test
    fun `an unrecognized or imported string maps to null rather than throwing`() {
        assertNull(ProgramDifficulty.levelSteps("some imported program's custom level string"))
    }
}
