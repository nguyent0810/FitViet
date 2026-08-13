package com.fitviet.app.domain

import com.fitviet.app.data.local.seed.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MonthlyPlanGeneratorTest {

    /** [SeedData.exercises] is the raw pre-insertion seed list — every entry has the default
     * `id = 0` since Room only assigns real distinct ids on actual insertion (this is exactly why
     * [MonthlyPlanGenerator] itself keys its curated allowlists by `nameVi`, never `id`, until a
     * real post-insertion catalog is passed in). [MonthlyPlanGenerator]'s own generation logic is
     * id-keyed throughout (matching the real post-insertion catalog it runs against in
     * production), so tests need a fixture with real distinct ids — this reassigns sequential
     * ones, standing in for what Room would actually hand back. */
    private val catalog = SeedData.exercises.mapIndexed { index, exercise -> exercise.copy(id = (index + 1).toLong()) }

    /** A real, fixed Monday — deterministic regardless of when the test runs. */
    private val monday = LocalDate.of(2024, 1, 1)

    private fun baseInput(
        splitTemplate: SplitTemplate,
        daysPerWeek: Int,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        level: ExerciseDifficulty = ExerciseDifficulty.INTERMEDIATE,
        totalWeeks: Int = 4,
        equipmentProfile: String? = null,
    ) = MonthlyPlanGenerationInput(
        goal = goal,
        level = level,
        splitTemplate = splitTemplate,
        daysPerWeek = daysPerWeek,
        totalWeeks = totalWeeks,
        sessionDurationTargetMinutes = 60,
        equipmentProfile = equipmentProfile,
        catalog = catalog,
        today = monday,
    )

    private fun trainingDays(draft: MonthlyPlanDraft, weekIndex: Int = 0) =
        draft.weeks[weekIndex].days.filterNot { it.isRestDay }

    @Test
    fun `PPL at 3 days a week places Push, Pull, Legs at offsets 0, 2, 4 from generation day`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.PPL, daysPerWeek = 3))
        val training = trainingDays(draft)

        // Redesign Gate 1d-i — asserted as offsets from `monday` (today at generation time), not
        // absolute weekdays: `monday` happening to actually be a Monday would otherwise let this
        // pass even if generate() silently regressed back to a fixed-weekday table, since Mon/
        // Wed/Fri offsets-from-Monday and offsets-from-today are identical in that one coincidental
        // case. See the non-Monday test below for the property that actually matters.
        assertEquals(listOf(0L, 2L, 4L), training.map { it.epochDay - monday.toEpochDay() })
        assertEquals(listOf("Push", "Pull", "Legs"), training.map { it.sessionType })
        training.forEach { assertTrue("${it.sessionType} should have exercises", it.exercises.isNotEmpty()) }
    }

    @Test
    fun `PPL at 6 days a week cycles Push, Pull, Legs twice, reusing the same exercises both times`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.PPL, daysPerWeek = 6))
        val training = trainingDays(draft)

        assertEquals(6, training.size)
        assertEquals(listOf("Push", "Pull", "Legs", "Push", "Pull", "Legs"), training.map { it.sessionType })
        // Exercise selection is fixed per session-type label for the whole block, not re-rolled
        // on each occurrence within the same week.
        assertEquals(training[0].exercises.map { it.exerciseId }, training[3].exercises.map { it.exerciseId })
    }

    @Test
    fun `Upper-Lower at 4 days a week lands on offsets 0, 1, 3, 4 from generation day`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.UPPER_LOWER, daysPerWeek = 4))
        val training = trainingDays(draft)

        assertEquals(listOf(0L, 1L, 3L, 4L), training.map { it.epochDay - monday.toEpochDay() })
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), training.map { it.sessionType })
    }

    @Test
    fun `today is always a trainable day, even when generation happens on a non-Monday`() {
        // Redesign Gate 1d-i's actual load-bearing property: offset 0 (today) must be trainable
        // regardless of what day of the week generation runs on — this is the one thing the
        // Monday-anchored fixture above can never prove, since every offset coincides with its old
        // absolute-weekday equivalent when `today` already is a Monday.
        val wednesday = LocalDate.of(2024, 1, 3)
        val input = baseInput(SplitTemplate.PPL, daysPerWeek = 3).copy(today = wednesday)

        val draft = MonthlyPlanGenerator.generate(input)
        val firstDay = draft.weeks[0].days.first()

        assertEquals(wednesday.toEpochDay(), firstDay.epochDay)
        assertFalse("generation day must be trainable, not a rest day", firstDay.isRestDay)
        assertEquals("Push", firstDay.sessionType)
        assertEquals(wednesday.toEpochDay(), draft.startEpochDay)
    }

    @Test
    fun `rotation continues across week boundaries when days per week does not match split length`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.UPPER_LOWER, daysPerWeek = 3))

        assertEquals(
            listOf("Upper A", "Lower A", "Upper B"),
            trainingDays(draft, weekIndex = 0).map { it.sessionType },
        )
        assertEquals(
            listOf("Lower B", "Upper A", "Lower A"),
            trainingDays(draft, weekIndex = 1).map { it.sessionType },
        )
    }

    @Test
    fun `custom slots with the same label keep their distinct muscle selections`() {
        val input = baseInput(SplitTemplate.CUSTOM, daysPerWeek = 2).copy(
            customSplitDays = listOf(
                CustomSplitDay("Workout", setOf(MuscleGroup.CHEST)),
                CustomSplitDay("Workout", setOf(MuscleGroup.LEGS)),
            ),
        )
        val training = trainingDays(MonthlyPlanGenerator.generate(input))
        val catalogById = catalog.associateBy { it.id }

        assertTrue(training[0].exercises.isNotEmpty())
        assertTrue(training[1].exercises.isNotEmpty())
        assertTrue(training[0].exercises.all { catalogById.getValue(it.exerciseId).muscleGroupCode == MuscleGroup.CHEST.name })
        assertTrue(training[1].exercises.all { catalogById.getValue(it.exerciseId).muscleGroupCode == MuscleGroup.LEGS.name })
    }

    @Test
    fun `Full Body at 3 days a week generates 3 sessions each covering multiple major groups`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.FULL_BODY, daysPerWeek = 3))
        val training = trainingDays(draft)

        assertEquals(3, training.size)
        training.forEach { day -> assertTrue(day.muscleGroupCodes.size >= 3) }
    }

    @Test
    fun `Advanced gets more exercises than Beginner for the same split and goal`() {
        val beginner = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.FULL_BODY, daysPerWeek = 3, level = ExerciseDifficulty.BEGINNER))
        val advanced = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.FULL_BODY, daysPerWeek = 3, level = ExerciseDifficulty.ADVANCED))

        val beginnerCount = trainingDays(beginner).first().exercises.size
        val advancedCount = trainingDays(advanced).first().exercises.size

        assertTrue(
            "advanced ($advancedCount) should have more exercises than beginner ($beginnerCount)",
            advancedCount > beginnerCount,
        )
    }

    @Test
    fun `computeExerciseCount matches the worked examples from the spec`() {
        assertTrue(
            MonthlyPlanGenerator.computeExerciseCount(TrainingGoal.GENERAL_FITNESS, ExerciseDifficulty.BEGINNER, muscleGroupCount = 4, sessionDurationTargetMinutes = 60, recentSetCountForPrimaryMuscle = 0) in 4..6,
        )
        assertTrue(
            MonthlyPlanGenerator.computeExerciseCount(TrainingGoal.HYPERTROPHY, ExerciseDifficulty.INTERMEDIATE, muscleGroupCount = 3, sessionDurationTargetMinutes = 60, recentSetCountForPrimaryMuscle = 0) in 5..7,
        )
        assertTrue(
            MonthlyPlanGenerator.computeExerciseCount(TrainingGoal.HYPERTROPHY, ExerciseDifficulty.ADVANCED, muscleGroupCount = 3, sessionDurationTargetMinutes = 90, recentSetCountForPrimaryMuscle = 0) in 6..9,
        )
    }

    @Test
    fun `high recent volume for the primary muscle biases the count toward the minimum`() {
        val rested = MonthlyPlanGenerator.computeExerciseCount(TrainingGoal.HYPERTROPHY, ExerciseDifficulty.INTERMEDIATE, muscleGroupCount = 3, sessionDurationTargetMinutes = 60, recentSetCountForPrimaryMuscle = 0)
        val fatigued = MonthlyPlanGenerator.computeExerciseCount(TrainingGoal.HYPERTROPHY, ExerciseDifficulty.INTERMEDIATE, muscleGroupCount = 3, sessionDurationTargetMinutes = 60, recentSetCountForPrimaryMuscle = 30)

        assertTrue(fatigued <= rested)
    }

    @Test
    fun `hypertrophy progression steps reps up each week then deloads, same exercise throughout`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.PPL, daysPerWeek = 3, goal = TrainingGoal.HYPERTROPHY, totalWeeks = 4))
        val pushDayByWeek = draft.weeks.map { week -> week.days.first { it.sessionType == "Push" } }
        val firstExerciseByWeek = pushDayByWeek.map { it.exercises.first() }

        assertEquals(1, firstExerciseByWeek.map { it.exerciseId }.distinct().size)

        val repsByWeek = firstExerciseByWeek.take(3).map { it.targetRepsMin } // BASE, BUILD, PEAK
        assertEquals(repsByWeek[0] + 1, repsByWeek[1])
        assertEquals(repsByWeek[1] + 1, repsByWeek[2])

        assertTrue("deload week should drop sets vs. the base week", firstExerciseByWeek[3].targetSets < firstExerciseByWeek[0].targetSets)
        assertEquals(firstExerciseByWeek[0].targetRepsMin, firstExerciseByWeek[3].targetRepsMin)
    }

    @Test
    fun `strength progression increases weight weekly and cuts it on deload`() {
        val draft = MonthlyPlanGenerator.generate(
            baseInput(SplitTemplate.PPL, daysPerWeek = 3, goal = TrainingGoal.STRENGTH, totalWeeks = 4)
                .copy(personalBests = catalog.associate { it.id to 40.0 }),
        )
        val pushDayByWeek = draft.weeks.map { week -> week.days.first { it.sessionType == "Push" } }
        val firstExerciseByWeek = pushDayByWeek.map { it.exercises.first() }

        val weightsByWeek = firstExerciseByWeek.take(3).mapNotNull { it.targetWeightKg }
        assertTrue(weightsByWeek[1] > weightsByWeek[0])
        assertTrue(weightsByWeek[2] > weightsByWeek[1])
        assertTrue("deload week should reduce weight vs. the base week", firstExerciseByWeek[3].targetWeightKg!! < firstExerciseByWeek[0].targetWeightKg!!)
    }

    @Test
    fun `a 5-week block inserts an extra BUILD phase and still ends in DELOAD`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.FULL_BODY, daysPerWeek = 3, totalWeeks = 5))

        assertEquals(listOf(PlanPhase.BASE, PlanPhase.BUILD, PlanPhase.BUILD, PlanPhase.PEAK, PlanPhase.DELOAD), draft.weeks.map { it.phase })
    }

    @Test
    fun `PHUL alternates power and hypertrophy rep styles regardless of the plan's own goal`() {
        val draft = MonthlyPlanGenerator.generate(
            baseInput(SplitTemplate.PHUL, daysPerWeek = 4, goal = TrainingGoal.HYPERTROPHY)
                .copy(personalBests = catalog.associate { it.id to 40.0 }),
        )
        val training = trainingDays(draft, weekIndex = 0)

        assertEquals(listOf("Power Upper", "Power Lower", "Hypertrophy Upper", "Hypertrophy Lower"), training.map { it.sessionType })
        val basePower = training[0].exercises.first()
        val buildPower = trainingDays(draft, weekIndex = 1)[0].exercises.first()
        val baseHypertrophy = training[2].exercises.first()
        val buildHypertrophy = trainingDays(draft, weekIndex = 1)[2].exercises.first()

        assertEquals(basePower.targetRepsMin, buildPower.targetRepsMin)
        assertTrue(buildPower.targetWeightKg!! > basePower.targetWeightKg!!)
        assertEquals(baseHypertrophy.targetRepsMin + 1, buildHypertrophy.targetRepsMin)
        assertEquals(baseHypertrophy.targetWeightKg, buildHypertrophy.targetWeightKg)
    }

    @Test
    fun `same input produces an identical plan every time`() {
        val input = baseInput(SplitTemplate.BRO_SPLIT, daysPerWeek = 5)

        assertEquals(MonthlyPlanGenerator.generate(input), MonthlyPlanGenerator.generate(input))
    }

    @Test
    fun `a no-equipment profile never picks an exercise requiring equipment`() {
        val draft = MonthlyPlanGenerator.generate(baseInput(SplitTemplate.PPL, daysPerWeek = 3, equipmentProfile = EquipmentProfiles.NO_EQUIPMENT))
        val catalogById = catalog.associateBy { it.id }

        val allPicked = draft.weeks.flatMap { it.days }.flatMap { it.exercises }
        assertTrue(allPicked.isNotEmpty())
        assertTrue(allPicked.all { EquipmentProfiles.allows(EquipmentProfiles.NO_EQUIPMENT, catalogById.getValue(it.exerciseId).equipment) })
    }

    @Test
    fun `every built-in split at 3 days a week under HOME_DUMBBELLS still assigns at least one exercise per training day`() {
        // MAIN_COMPOUND_NAMES is barbell/machine-heavy (Bench Press, Bent-Over Row, Squat,
        // Deadlift, Shoulder Press, Barbell Curl, Triceps Pushdown) — none of those are reachable
        // under HOME_DUMBBELLS, so this only passes if pickExercise's fallbackTier chain
        // (MAIN_COMPOUND -> SECONDARY_COMPOUND -> TARGETED_ACCESSORY -> ...) actually finds a
        // dumbbell-compatible substitute for every muscle group rather than leaving a slot empty.
        val builtInTemplates = SplitTemplate.entries.filterNot { it == SplitTemplate.CUSTOM }
        for (template in builtInTemplates) {
            val draft = MonthlyPlanGenerator.generate(
                baseInput(template, daysPerWeek = 3, equipmentProfile = EquipmentProfiles.HOME_DUMBBELLS),
            )
            val training = trainingDays(draft)
            assertTrue("$template should have training days", training.isNotEmpty())
            training.forEach { day ->
                assertTrue(
                    "$template's ${day.sessionType} should have at least one exercise under HOME_DUMBBELLS",
                    day.exercises.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `an unsupported daysPerWeek is rejected rather than silently producing a wrong plan`() {
        try {
            MonthlyPlanGenerator.generate(baseInput(SplitTemplate.PPL, daysPerWeek = 7))
            org.junit.Assert.fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}
