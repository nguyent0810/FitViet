package com.fitviet.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramTransferTest {

    private val sample = ProgramTransferData(
        titleVi = "Giáo án test",
        durationWeeks = 8,
        sessionsPerWeek = 4,
        level = "Trung cấp",
        equipment = "Phòng gym",
        days = listOf(
            ProgramTransferDay(
                dayOfWeek = 1,
                titleVi = "Ngực & Vai",
                isRestDay = false,
                exercises = listOf(
                    ProgramTransferExercise("Đẩy ngực tạ đòn", 4, 8, 12),
                    ProgramTransferExercise("Đẩy vai", 3, 8, 10),
                ),
            ),
            ProgramTransferDay(dayOfWeek = 2, titleVi = "Nghỉ", isRestDay = true, exercises = emptyList()),
        ),
    )

    @Test
    fun `encode then decode round-trips exactly`() {
        val decoded = ProgramTransfer.decode(ProgramTransfer.encode(sample))
        assertEquals(sample, decoded)
    }

    @Test
    fun `decode rejects garbage input`() {
        assertNull(ProgramTransfer.decode("not json at all"))
    }

    @Test
    fun `decode rejects valid JSON with wrong format tag`() {
        assertNull(ProgramTransfer.decode("""{"format":"some-other-app-v1","titleVi":"x"}"""))
    }

    @Test
    fun `decode rejects a day-of-week outside 1 to 7`() {
        val bad = """
            {"format":"fitviet-program-v1","titleVi":"x","durationWeeks":1,"sessionsPerWeek":1,
             "level":"l","equipment":"e","days":[{"dayOfWeek":8,"titleVi":"x","isRestDay":false,"exercises":[]}]}
        """.trimIndent()
        assertNull(ProgramTransfer.decode(bad))
    }

    @Test
    fun `decode rejects duplicate day-of-week values`() {
        // Would otherwise violate ProgramDayEntity's unique (programId, dayOfWeek) index mid-import.
        val bad = """
            {"format":"fitviet-program-v1","titleVi":"x","durationWeeks":1,"sessionsPerWeek":1,
             "level":"l","equipment":"e","days":[
               {"dayOfWeek":1,"titleVi":"a","isRestDay":false,"exercises":[]},
               {"dayOfWeek":1,"titleVi":"b","isRestDay":false,"exercises":[]}
             ]}
        """.trimIndent()
        assertNull(ProgramTransfer.decode(bad))
    }

    @Test
    fun `decode rejects JSON missing a required field`() {
        val bad = """{"format":"fitviet-program-v1","titleVi":"x","days":[]}"""
        assertNull(ProgramTransfer.decode(bad))
    }

    @Test
    fun `decode handles a program with no days`() {
        val data = sample.copy(days = emptyList())
        assertEquals(data, ProgramTransfer.decode(ProgramTransfer.encode(data)))
    }
}
