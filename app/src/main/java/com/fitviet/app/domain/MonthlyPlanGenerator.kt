package com.fitviet.app.domain

import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.seed.SeedExerciseNames
import com.fitviet.app.ui.workout.DEFAULT_REST_SECONDS
import com.fitviet.app.ui.workout.SECONDS_PER_REP
import com.fitviet.app.ui.workout.TRANSITION_SECONDS
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.round

/** One position in a split template's weekly rotation — e.g. PPL's "Push" slot. [repStyleOverride]
 * is only set for PHUL, whose Power/Hypertrophy sessions use different progression styles
 * regardless of the plan's overall [TrainingGoal]; every other template leaves it null (use the
 * plan's own goal). */
data class SessionTypeSlot(
    val label: String,
    val muscleGroups: Set<MuscleGroup>,
    val repStyleOverride: TrainingGoal? = null,
)

/** One user-authored day in a Custom Split — [SplitTemplate.CUSTOM]'s day-count/muscle-group
 * choices, decoded from [com.fitviet.app.data.local.entity.SplitTemplateEntity.dayDefinitionsJson]
 * by the repository before reaching the generator (this file stays JSON/Room-free). */
data class CustomSplitDay(val label: String, val muscleGroups: Set<MuscleGroup>)

data class MonthlyPlanGenerationInput(
    val goal: TrainingGoal,
    val level: ExerciseDifficulty,
    val splitTemplate: SplitTemplate,
    val daysPerWeek: Int,
    /** 4 or 5. */
    val totalWeeks: Int,
    /** Required, non-empty, only when [splitTemplate] == CUSTOM. */
    val customSplitDays: List<CustomSplitDay>? = null,
    /** [EquipmentProfiles] key, or null = unconstrained. */
    val equipmentProfile: String? = null,
    val sessionDurationTargetMinutes: Int,
    val exclusionExerciseIds: Set<Long> = emptySet(),
    /** [MuscleGroup.name] values — a session whose every muscle group is excluded generates with
     * zero exercises for that slot rather than crashing (a degenerate but valid input). */
    val excludedMuscleGroupCodes: Set<String> = emptySet(),
    val catalog: List<ExerciseEntity>,
    /** exerciseId -> known max weightKg, read once at generation time (see the "Hit & Run" plan's
     * PR-integration section — this function never re-checks PRs itself). */
    val personalBests: Map<Long, Double> = emptyMap(),
    /** [MuscleGroup.name] -> trailing set count, feeds the exercise-count recovery adjustment. */
    val recentSetCountByMuscleGroup: Map<String, Int> = emptyMap(),
    /** exerciseId -> most recent completed-session date, feeds the variety soft-rank. */
    val recentExerciseUsage: Map<Long, LocalDate> = emptyMap(),
    val today: LocalDate,
)

/**
 * "Hit & Run" one-tap monthly plan generator. Pure, deterministic, no Room/Android import — same
 * idiom as [NextTrainingCalculator]/[WorkoutCompositionCalculator]/[ProgramScheduleCalculator].
 * [com.fitviet.app.data.repository.MonthlyPlanRepository] persists [generate]'s output
 * transactionally; nothing here touches the database.
 *
 * Reuses [SECONDS_PER_REP]/[TRANSITION_SECONDS]/[DEFAULT_REST_SECONDS] from
 * `com.fitviet.app.ui.workout` (internal, module-visible, no Android dependency themselves) rather
 * than duplicating them, so this generator's session-duration estimate can never silently drift
 * from [com.fitviet.app.ui.workout.WorkoutTimeBudgetPlanner]'s or
 * [ProgramDayWorkoutPlanner.estimateDurationMinutes]'s.
 *
 * Exercise selection is fixed once per distinct session-type label for the whole 4-5 week block —
 * only sets/reps/weight vary week to week (see [applyProgression]) — because the spec's own
 * progression example ("Bench Press: W1 3x8, W2 3x9...") assumes the same exercise persists.
 * Cross-generation variety comes from the recency soft-rank in [pickExercise], not from
 * re-selecting exercises mid-block.
 */
object MonthlyPlanGenerator {

    fun generate(input: MonthlyPlanGenerationInput): MonthlyPlanDraft {
        require(input.daysPerWeek in SPACING_TABLE) { "unsupported daysPerWeek: ${input.daysPerWeek}" }
        require(input.totalWeeks == 4 || input.totalWeeks == 5) { "totalWeeks must be 4 or 5, was ${input.totalWeeks}" }

        val rotation = rotationFor(input.splitTemplate, input.customSplitDays)
        val trainingDows = SPACING_TABLE.getValue(input.daysPerWeek)
        // Week 1 always anchors to the current calendar week, even mid-week — days already past
        // in week 1 are simply inert (MissedWorkoutDetector only scans SCHEDULED days going
        // forward from a live plan, so a day that was never reachable is never flagged missed).
        val startMonday = input.today.with(DayOfWeek.MONDAY)
        val phases = phaseTableFor(input.totalWeeks)

        val usedThisGeneration = input.recentExerciseUsage.toMutableMap()
        // Key by rotation position, not display label: custom splits may legitimately reuse a
        // label for two slots with different muscle-group definitions.
        val exercisesBySlot: List<List<ExercisePick>> = rotation.map { slot ->
            val picks = buildSessionExercises(input, slot, usedThisGeneration)
            picks.forEach { usedThisGeneration[it.exerciseId] = input.today }
            picks
        }

        var rotationIndex = 0
        val weeks = phases.mapIndexed { weekIndex, phase ->
            val weekMonday = startMonday.plusWeeks(weekIndex.toLong())
            val days = DayOfWeek.values().map { dow ->
                val date = weekMonday.plusDays((dow.value - 1).toLong())
                if (dow !in trainingDows) {
                    MonthlyPlanDayDraft(
                        epochDay = date.toEpochDay(),
                        dayOfWeek = dow,
                        isRestDay = true,
                        sessionType = null,
                        muscleGroupCodes = emptyList(),
                        exercises = emptyList(),
                    )
                } else {
                    val slotIndex = rotationIndex % rotation.size
                    val slot = rotation[slotIndex]
                    rotationIndex++
                    val basePicks = exercisesBySlot[slotIndex]
                    val exercises = basePicks.map { pick ->
                        applyProgression(pick, input.personalBests[pick.exerciseId], input.goal, slot.repStyleOverride, phase, weekIndex)
                    }
                    MonthlyPlanDayDraft(
                        epochDay = date.toEpochDay(),
                        dayOfWeek = dow,
                        isRestDay = false,
                        sessionType = slot.label,
                        muscleGroupCodes = slot.muscleGroups.map { it.name },
                        exercises = exercises,
                    )
                }
            }
            MonthlyPlanWeekDraft(weekIndex, phase, days)
        }

        return MonthlyPlanDraft(
            goal = input.goal,
            level = input.level,
            splitTemplate = input.splitTemplate,
            daysPerWeek = input.daysPerWeek,
            totalWeeks = input.totalWeeks,
            startEpochDay = startMonday.toEpochDay(),
            equipmentProfile = input.equipmentProfile,
            exclusionExerciseIds = input.exclusionExerciseIds,
            excludedMuscleGroupCodes = input.excludedMuscleGroupCodes,
            sessionDurationTargetMinutes = input.sessionDurationTargetMinutes,
            weeks = weeks,
        )
    }

    /** Regenerates ONE day's exercises in isolation — the "Hit & Run" plan's single-day regenerate
     * action. Unlike [generate]'s whole-block run (where one exercise list is picked once and
     * reused by every week sharing that slot's label), this deliberately re-picks for just the one
     * requested (slot, phase, weekIndex) — an explicit "give me something different for this one
     * day" user action isn't expected to silently resync every other week of the same label.
     * [input.exclusionExerciseIds] should already include that day's currently-assigned exercise
     * ids (the repository's job) so the reselection actually changes something instead of picking
     * the same exercises again. */
    fun generateDayExercises(
        input: MonthlyPlanGenerationInput,
        slot: SessionTypeSlot,
        phase: PlanPhase,
        weekIndex: Int,
        onlyTier: ExerciseTier? = null,
    ): List<MonthlyPlanExerciseDraft> {
        val picks = if (onlyTier == null) {
            buildSessionExercises(input, slot, input.recentExerciseUsage.toMutableMap())
        } else {
            val group = slot.muscleGroups.singleOrNull() ?: return emptyList()
            listOfNotNull(pickExercise(input, group, onlyTier, emptySet(), input.recentExerciseUsage))
        }
        return picks.map { pick -> applyProgression(pick, input.personalBests[pick.exerciseId], input.goal, slot.repStyleOverride, phase, weekIndex) }
    }

    /** Looks up a template's [SessionTypeSlot] by its display label — lets the repository
     * reconstruct the exact muscle-group set and PHUL rep-style override for a single day's
     * regenerate without duplicating [rotationFor]'s per-template tables. Returns null if
     * [label] doesn't match any slot in this template (shouldn't happen for a label this
     * generator itself produced, but callers should treat null as "can't regenerate," not crash). */
    fun slotFor(splitTemplate: SplitTemplate, customSplitDays: List<CustomSplitDay>?, label: String): SessionTypeSlot? =
        rotationFor(splitTemplate, customSplitDays).firstOrNull { it.label == label }

    // ---- (a) day-of-week assignment ----

    private val SPACING_TABLE: Map<Int, Set<DayOfWeek>> = mapOf(
        2 to setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        3 to setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        4 to setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        5 to setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
        6 to setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
        ),
    )

    private fun rotationFor(template: SplitTemplate, custom: List<CustomSplitDay>?): List<SessionTypeSlot> = when (template) {
        SplitTemplate.PPL -> listOf(
            SessionTypeSlot("Push", setOf(MuscleGroup.CHEST, MuscleGroup.DELTOIDS, MuscleGroup.TRICEPS)),
            SessionTypeSlot("Pull", setOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARM)),
            SessionTypeSlot("Legs", setOf(MuscleGroup.LEGS, MuscleGroup.GLUTEUS, MuscleGroup.ABS)),
        )
        SplitTemplate.UPPER_LOWER -> listOf(
            SessionTypeSlot("Upper A", upperGroups()),
            SessionTypeSlot("Lower A", lowerGroups()),
            SessionTypeSlot("Upper B", upperGroups()),
            SessionTypeSlot("Lower B", lowerGroups()),
        )
        SplitTemplate.FULL_BODY -> listOf(
            SessionTypeSlot("Full Body A", setOf(MuscleGroup.LEGS, MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.ABS)),
            SessionTypeSlot("Full Body B", setOf(MuscleGroup.BACK, MuscleGroup.CHEST, MuscleGroup.LEGS, MuscleGroup.DELTOIDS)),
            SessionTypeSlot("Full Body C", setOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.LEGS, MuscleGroup.TRICEPS)),
            SessionTypeSlot("Full Body D", setOf(MuscleGroup.DELTOIDS, MuscleGroup.LEGS, MuscleGroup.BACK, MuscleGroup.BICEPS)),
        )
        SplitTemplate.BRO_SPLIT -> listOf(
            SessionTypeSlot("Chest", setOf(MuscleGroup.CHEST)),
            SessionTypeSlot("Back", setOf(MuscleGroup.BACK)),
            SessionTypeSlot("Legs", setOf(MuscleGroup.LEGS, MuscleGroup.GLUTEUS)),
            SessionTypeSlot("Shoulders", setOf(MuscleGroup.DELTOIDS)),
            SessionTypeSlot("Arms", setOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS)),
        )
        SplitTemplate.PHUL -> listOf(
            SessionTypeSlot("Power Upper", setOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.DELTOIDS), TrainingGoal.STRENGTH),
            SessionTypeSlot("Power Lower", setOf(MuscleGroup.LEGS, MuscleGroup.GLUTEUS), TrainingGoal.STRENGTH),
            SessionTypeSlot("Hypertrophy Upper", upperGroups(), TrainingGoal.HYPERTROPHY),
            SessionTypeSlot("Hypertrophy Lower", lowerGroups(), TrainingGoal.HYPERTROPHY),
        )
        SplitTemplate.CUSTOM -> {
            requireNotNull(custom) { "CUSTOM split requires customSplitDays" }
            require(custom.isNotEmpty()) { "CUSTOM split requires at least one day definition" }
            custom.map { SessionTypeSlot(it.label, it.muscleGroups) }
        }
    }

    private fun upperGroups() = setOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.DELTOIDS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
    private fun lowerGroups() = setOf(MuscleGroup.LEGS, MuscleGroup.GLUTEUS, MuscleGroup.ABS)

    // ---- (b) exercise count ----

    private val BASE_COUNT_RANGE: Map<Pair<TrainingGoal, ExerciseDifficulty>, IntRange> = mapOf(
        (TrainingGoal.GENERAL_FITNESS to ExerciseDifficulty.BEGINNER) to (4..6),
        (TrainingGoal.GENERAL_FITNESS to ExerciseDifficulty.INTERMEDIATE) to (5..7),
        (TrainingGoal.GENERAL_FITNESS to ExerciseDifficulty.ADVANCED) to (6..8),
        (TrainingGoal.HYPERTROPHY to ExerciseDifficulty.BEGINNER) to (5..6),
        (TrainingGoal.HYPERTROPHY to ExerciseDifficulty.INTERMEDIATE) to (5..7),
        (TrainingGoal.HYPERTROPHY to ExerciseDifficulty.ADVANCED) to (6..9),
        (TrainingGoal.STRENGTH to ExerciseDifficulty.BEGINNER) to (4..5),
        (TrainingGoal.STRENGTH to ExerciseDifficulty.INTERMEDIATE) to (4..6),
        (TrainingGoal.STRENGTH to ExerciseDifficulty.ADVANCED) to (5..7),
    )

    private fun recoveryThresholdFor(level: ExerciseDifficulty): Int = when (level) {
        ExerciseDifficulty.BEGINNER -> 12
        ExerciseDifficulty.INTERMEDIATE -> 16
        ExerciseDifficulty.ADVANCED -> 20
    }

    private fun midpointRepsFor(goal: TrainingGoal): Int = when (goal) {
        TrainingGoal.STRENGTH -> 5
        TrainingGoal.HYPERTROPHY -> 10
        TrainingGoal.GENERAL_FITNESS -> 10
    }

    /** Solves `count * (avgSets*avgReps*SECONDS_PER_REP + (avgSets-1)*DEFAULT_REST_SECONDS +
     * TRANSITION_SECONDS) <= sessionDurationTargetMinutes*60` for count, using the goal's
     * midpoint rep target and a flat 3-set assumption per exercise as the estimate's average
     * block shape — the same constants [com.fitviet.app.ui.workout.WorkoutTimeBudgetPlanner]
     * already uses for its own greedy time-budget fill. */
    private fun durationImpliedMaxCount(goal: TrainingGoal, durationTargetMinutes: Int): Int {
        val avgReps = midpointRepsFor(goal)
        val avgSets = 3
        val blockSeconds = avgSets * avgReps * SECONDS_PER_REP + (avgSets - 1) * DEFAULT_REST_SECONDS + TRANSITION_SECONDS
        return (durationTargetMinutes * 60 / blockSeconds).coerceAtLeast(3)
    }

    fun computeExerciseCount(
        goal: TrainingGoal,
        level: ExerciseDifficulty,
        muscleGroupCount: Int,
        sessionDurationTargetMinutes: Int,
        recentSetCountForPrimaryMuscle: Int,
    ): Int {
        val base = BASE_COUNT_RANGE.getValue(goal to level)
        val breadthBonus = (muscleGroupCount - 2).coerceIn(0, 3)
        val durationMax = durationImpliedMaxCount(goal, sessionDurationTargetMinutes)
        val maxCount = minOf(base.last + breadthBonus, durationMax)
        val minCount = base.first
        val recovering = recentSetCountForPrimaryMuscle > recoveryThresholdFor(level)
        val picked = if (recovering || maxCount <= minCount) minCount else ceil((minCount + maxCount) / 2.0).toInt()
        return picked.coerceIn(3, 10)
    }

    // ---- (c) exercise selection ----

    /** Curated "big lift" allowlist per muscle group, reusing [SeedExerciseNames] — same idiom as
     * [com.fitviet.app.ui.workout.WorkoutTimeBudgetPlanner.CURRICULUM_ORDER]. Stands in for a real
     * [ExerciseEntity.tierCode] classification column, deferred per the "Hit & Run" plan's scope
     * note; groups with no curated entry here (FOREARM/ABS/FUNCTIONAL/CARDIO/STRETCHING) simply
     * never produce a MAIN_COMPOUND pick and fall through to SECONDARY_COMPOUND/accessory tiers. */
    private val MAIN_COMPOUND_NAMES: Map<MuscleGroup, List<String>> = mapOf(
        MuscleGroup.CHEST to listOf(SeedExerciseNames.BENCH_PRESS),
        MuscleGroup.BACK to listOf(SeedExerciseNames.BENT_OVER_ROW, SeedExerciseNames.LAT_PULLDOWN, SeedExerciseNames.DEADLIFT),
        MuscleGroup.LEGS to listOf(SeedExerciseNames.SQUAT, SeedExerciseNames.LEG_PRESS, SeedExerciseNames.DEADLIFT),
        MuscleGroup.GLUTEUS to listOf(SeedExerciseNames.SQUAT, SeedExerciseNames.DEADLIFT),
        MuscleGroup.DELTOIDS to listOf(SeedExerciseNames.SHOULDER_PRESS),
        MuscleGroup.BICEPS to listOf(SeedExerciseNames.BARBELL_CURL),
        MuscleGroup.TRICEPS to listOf(SeedExerciseNames.TRICEPS_PUSHDOWN),
    )

    private data class ExercisePick(
        val exerciseId: Long,
        val orderIndex: Int,
        val tier: ExerciseTier,
        val reasonCode: String,
        val exercise: ExerciseEntity,
    )

    private fun buildSessionExercises(
        input: MonthlyPlanGenerationInput,
        slot: SessionTypeSlot,
        usedThisGeneration: MutableMap<Long, LocalDate>,
    ): List<ExercisePick> {
        val groups = slot.muscleGroups.filterNot { it.name in input.excludedMuscleGroupCodes }
        if (groups.isEmpty()) return emptyList()
        val primaryGroup = groups.first()
        val count = computeExerciseCount(
            goal = input.goal,
            level = input.level,
            muscleGroupCount = groups.size,
            sessionDurationTargetMinutes = input.sessionDurationTargetMinutes,
            recentSetCountForPrimaryMuscle = input.recentSetCountByMuscleGroup[primaryGroup.name] ?: 0,
        )
        val tierPlan = buildTierPlan(groups, count, input.goal)
        val picked = mutableSetOf<Long>()
        val picks = mutableListOf<ExercisePick>()
        for ((group, tier) in tierPlan) {
            val pick = pickExercise(input, group, tier, picked, usedThisGeneration) ?: continue
            picked += pick.exerciseId
            picks += pick
        }
        return picks.mapIndexed { index, pick -> pick.copy(orderIndex = index) }
    }

    /** Assigns each of [count] slots a (muscleGroup, tier) target: 1 MAIN_COMPOUND for the
     * session's first-priority group, 1 SECONDARY_COMPOUND for its second (if any budget/groups
     * remain), then TARGETED_ACCESSORY cycling through every remaining group at least once,
     * ISOLATION for any further budget (also cycling), and — only if exactly 1 slot is left after
     * every group has at least one pick and [goal] isn't STRENGTH — a FINISHER. */
    private fun buildTierPlan(groups: List<MuscleGroup>, count: Int, goal: TrainingGoal): List<Pair<MuscleGroup, ExerciseTier>> {
        if (count <= 0) return emptyList()
        val plan = mutableListOf<Pair<MuscleGroup, ExerciseTier>>()
        val covered = mutableSetOf<Int>()

        plan += groups[0] to ExerciseTier.MAIN_COMPOUND
        covered += 0
        if (plan.size < count && groups.size > 1) {
            plan += groups[1] to ExerciseTier.SECONDARY_COMPOUND
            covered += 1
        }

        // Start with the first group not already covered by the compound slots. Starting again
        // at zero would spend accessory budget on the primary group before the remaining groups
        // receive even one exercise (for example, Push would revisit chest before triceps).
        var cursor = covered.size
        while (plan.size < count) {
            val remaining = count - plan.size
            if (remaining == 1 && goal != TrainingGoal.STRENGTH && covered.size >= groups.size) {
                plan += groups[0] to ExerciseTier.FINISHER
                break
            }
            val groupIndex = cursor % groups.size
            val tier = if (groupIndex !in covered) ExerciseTier.TARGETED_ACCESSORY else ExerciseTier.ISOLATION
            covered += groupIndex
            plan += groups[groupIndex] to tier
            cursor++
        }
        return plan
    }

    private fun filterByTier(candidates: List<ExerciseEntity>, group: MuscleGroup, tier: ExerciseTier): List<ExerciseEntity> = when (tier) {
        ExerciseTier.MAIN_COMPOUND -> candidates.filter { it.nameVi in MAIN_COMPOUND_NAMES[group].orEmpty() }
        ExerciseTier.SECONDARY_COMPOUND -> candidates.filter {
            it.movementType == MovementType.COMPOUND.name && it.nameVi !in MAIN_COMPOUND_NAMES[group].orEmpty()
        }
        ExerciseTier.TARGETED_ACCESSORY, ExerciseTier.ISOLATION, ExerciseTier.FINISHER ->
            candidates.filter { it.movementType == MovementType.ISOLATION.name }
    }

    /** When a session's desired tier has no catalog match for this muscle group (small groups
     * like FOREARM often have none MAIN_COMPOUND), fall back to the next-loosest tier rather than
     * leaving the muscle group uncovered entirely. */
    private fun fallbackTier(tier: ExerciseTier): ExerciseTier = when (tier) {
        ExerciseTier.MAIN_COMPOUND -> ExerciseTier.SECONDARY_COMPOUND
        ExerciseTier.SECONDARY_COMPOUND -> ExerciseTier.TARGETED_ACCESSORY
        ExerciseTier.TARGETED_ACCESSORY -> ExerciseTier.ISOLATION
        ExerciseTier.ISOLATION -> ExerciseTier.SECONDARY_COMPOUND
        ExerciseTier.FINISHER -> ExerciseTier.ISOLATION
    }

    private fun pickExercise(
        input: MonthlyPlanGenerationInput,
        group: MuscleGroup,
        tier: ExerciseTier,
        alreadyPicked: Set<Long>,
        usedRecently: Map<Long, LocalDate>,
    ): ExercisePick? {
        val baseCandidates = input.catalog.filter {
            it.muscleGroupCode == group.name &&
                it.id !in input.exclusionExerciseIds &&
                it.id !in alreadyPicked &&
                EquipmentProfiles.allows(input.equipmentProfile, it.equipment)
        }
        if (baseCandidates.isEmpty()) return null

        val tierFiltered = filterByTier(baseCandidates, group, tier)
            .ifEmpty { filterByTier(baseCandidates, group, fallbackTier(tier)) }
            .ifEmpty { baseCandidates }

        // Recency soft-rank doubles as both the "fatigue" and "variety" ranks the design calls out
        // separately — the only per-exercise signal actually available is last-used date
        // (RecentExerciseUsageRow), not a per-exercise volume figure; never-used sorts first
        // (Long.MIN_VALUE), then oldest-used, then a deterministic id tie-break so the generator
        // stays a pure, reproducible function of its inputs.
        val chosen = tierFiltered.sortedWith(
            compareBy(
                { usedRecently[it.id]?.toEpochDay() ?: Long.MIN_VALUE },
                { it.id },
            ),
        ).first()

        val equipmentTag = input.equipmentProfile?.let { "equip_$it" } ?: "equip_unconstrained"
        val usageTag = usedRecently[chosen.id]?.let { "last_used_$it" } ?: "not_recently_used"
        return ExercisePick(
            exerciseId = chosen.id,
            orderIndex = 0,
            tier = tier,
            reasonCode = "${tier.name}|${group.name}|$equipmentTag|$usageTag",
            exercise = chosen,
        )
    }

    // ---- (d) week-to-week progression ----

    private fun phaseTableFor(totalWeeks: Int): List<PlanPhase> = when (totalWeeks) {
        4 -> listOf(PlanPhase.BASE, PlanPhase.BUILD, PlanPhase.PEAK, PlanPhase.DELOAD)
        5 -> listOf(PlanPhase.BASE, PlanPhase.BUILD, PlanPhase.BUILD, PlanPhase.PEAK, PlanPhase.DELOAD)
        else -> error("totalWeeks must be 4 or 5, was $totalWeeks")
    }

    private fun midpoint(min: Int, max: Int): Int = round((min + max) / 2.0).toInt().coerceAtLeast(1)

    private fun incrementKgFor(exercise: ExerciseEntity): Double = when (exercise.muscleGroupCode) {
        MuscleGroup.LEGS.name, MuscleGroup.GLUTEUS.name, MuscleGroup.BACK.name -> 5.0
        else -> 2.5
    }

    /** HYPERTROPHY: reps step up by 1 each non-deload week, clamped to the exercise's own
     * suggested max (matches the spec's Bench Press example: W1 3x8, W2 3x9, W3 3x10 — weight
     * flat except for the separate post-session PR-bump hook); deload drops 1 set and resets reps
     * to the block's base. */
    private fun hypertrophyProgression(
        baseSets: Int, repsMin: Int, repsMax: Int, seedWeight: Double?, phase: PlanPhase, weekIndex: Int,
    ): Triple<Int, Pair<Int, Int>, Double?> = if (phase == PlanPhase.DELOAD) {
        Triple((baseSets - 1).coerceAtLeast(2), repsMin to repsMin, seedWeight)
    } else {
        val reps = (repsMin + weekIndex).coerceAtMost(repsMax)
        Triple(baseSets, reps to reps, seedWeight)
    }

    /** STRENGTH: reps held near the block's base every non-deload week, weight increases by a
     * small pattern-based increment each week; deload cuts weight ~20% and drops 1 set. */
    private fun strengthProgression(
        baseSets: Int, repsMin: Int, seedWeight: Double?, incrementKg: Double, phase: PlanPhase, weekIndex: Int,
    ): Triple<Int, Pair<Int, Int>, Double?> = if (phase == PlanPhase.DELOAD) {
        Triple((baseSets - 1).coerceAtLeast(2), repsMin to repsMin, seedWeight?.times(0.8))
    } else {
        Triple(baseSets, repsMin to repsMin, seedWeight?.plus(incrementKg * weekIndex))
    }

    /** GENERAL_FITNESS: blends both styles — reps step up like HYPERTROPHY, but deload is more
     * pronounced (both sets and reps drop, not just sets). */
    private fun generalFitnessProgression(
        baseSets: Int, repsMin: Int, repsMax: Int, seedWeight: Double?, phase: PlanPhase, weekIndex: Int,
    ): Triple<Int, Pair<Int, Int>, Double?> = if (phase == PlanPhase.DELOAD) {
        val deloadReps = (repsMin - 2).coerceAtLeast(1)
        Triple((baseSets - 2).coerceAtLeast(2), deloadReps to deloadReps, seedWeight)
    } else {
        val reps = (repsMin + weekIndex).coerceAtMost(repsMax)
        Triple(baseSets, reps to reps, seedWeight)
    }

    private fun applyProgression(
        pick: ExercisePick,
        seedWeight: Double?,
        planGoal: TrainingGoal,
        repStyleOverride: TrainingGoal?,
        phase: PlanPhase,
        weekIndex: Int,
    ): MonthlyPlanExerciseDraft {
        val goal = repStyleOverride ?: planGoal
        val exercise = pick.exercise
        val baseSets = midpoint(exercise.suggestedSetsMin, exercise.suggestedSetsMax)
        val (sets, reps, weight) = when (goal) {
            TrainingGoal.HYPERTROPHY -> hypertrophyProgression(baseSets, exercise.suggestedRepsMin, exercise.suggestedRepsMax, seedWeight, phase, weekIndex)
            TrainingGoal.STRENGTH -> strengthProgression(baseSets, exercise.suggestedRepsMin, seedWeight, incrementKgFor(exercise), phase, weekIndex)
            TrainingGoal.GENERAL_FITNESS -> generalFitnessProgression(baseSets, exercise.suggestedRepsMin, exercise.suggestedRepsMax, seedWeight, phase, weekIndex)
        }
        return MonthlyPlanExerciseDraft(
            exerciseId = pick.exerciseId,
            orderIndex = pick.orderIndex,
            tier = pick.tier,
            selectionReasonCode = pick.reasonCode,
            targetSets = sets,
            targetRepsMin = reps.first,
            targetRepsMax = reps.second,
            targetWeightKg = weight,
        )
    }
}
