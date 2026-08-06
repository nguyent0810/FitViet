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

    @Test
    fun `decode rejects a non-positive durationWeeks or sessionsPerWeek`() {
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(durationWeeks = 0))))
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(sessionsPerWeek = -1))))
    }

    @Test
    fun `decode rejects blank program, day, or exercise names`() {
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(titleVi = "  "))))
        assertNull(
            ProgramTransfer.decode(
                ProgramTransfer.encode(sample.copy(days = listOf(sample.days[0].copy(titleVi = ""), sample.days[1]))),
            ),
        )
        val blankExerciseName = sample.days[0].copy(exercises = sample.days[0].exercises.map { it.copy(nameVi = " ") })
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(days = listOf(blankExerciseName, sample.days[1])))))
    }

    @Test
    fun `decode rejects non-positive or inverted set-rep targets`() {
        val zeroSets = sample.days[0].copy(exercises = sample.days[0].exercises.map { it.copy(targetSets = 0) })
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(days = listOf(zeroSets, sample.days[1])))))

        val invertedReps = sample.days[0].copy(exercises = sample.days[0].exercises.map { it.copy(targetRepsMin = 12, targetRepsMax = 8) })
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(days = listOf(invertedReps, sample.days[1])))))
    }

    @Test
    fun `decode rejects a training day with no exercises`() {
        val emptyTrainingDay = sample.days[0].copy(exercises = emptyList())
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(days = listOf(emptyTrainingDay, sample.days[1])))))
    }

    @Test
    fun `decode rejects a rest day with exercises`() {
        val restDayWithExercises = sample.days[1].copy(exercises = sample.days[0].exercises)
        assertNull(ProgramTransfer.decode(ProgramTransfer.encode(sample.copy(days = listOf(sample.days[0], restDayWithExercises)))))
    }

    @Test
    fun `a well-formed sample still round-trips under the new validation rules`() {
        // Pure domain-layer check only -- this doesn't exercise ProgramRepository.exportProgram()
        // itself (this project doesn't Room-test repositories). The actual guarantee that a real
        // export can never violate these rules -- specifically, that an imported training day
        // can't end up with zero resolved exercises after unmatched names are dropped -- lives in
        // ProgramRepository.importTransaction(), which drops a training day entirely rather than
        // inserting one with no exercises; see that function's own doc comment.
        assertEquals(sample, ProgramTransfer.decode(ProgramTransfer.encode(sample)))
    }
}
