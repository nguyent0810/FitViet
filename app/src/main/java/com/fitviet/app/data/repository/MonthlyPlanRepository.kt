package com.fitviet.app.data.repository

import androidx.room.withTransaction
import com.fitviet.app.data.local.FitVietDatabase
import com.fitviet.app.data.local.dao.ExerciseDao
import com.fitviet.app.data.local.dao.MonthlyPlanDao
import com.fitviet.app.data.local.dao.MonthlyPlanDayDao
import com.fitviet.app.data.local.dao.MonthlyPlanExerciseDao
import com.fitviet.app.data.local.dao.MonthlyPlanWeekDao
import com.fitviet.app.data.local.dao.SetLogDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.dao.SplitTemplateDao
import com.fitviet.app.data.local.dao.WorkoutSessionDao
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.MonthlyPlanExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import com.fitviet.app.data.local.entity.SettingsEntity
import com.fitviet.app.data.local.entity.SplitTemplateEntity
import com.fitviet.app.domain.CompletedSet
import com.fitviet.app.domain.CustomSplitCodec
import com.fitviet.app.domain.CustomSplitDay
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.MissedWorkoutDetector
import com.fitviet.app.domain.MonthlyPlanDayCardEstimator
import com.fitviet.app.domain.MonthlyPlanDayStatus
import com.fitviet.app.domain.MonthlyPlanExerciseDraft
import com.fitviet.app.domain.MonthlyPlanGenerationInput
import com.fitviet.app.domain.MonthlyPlanGenerator
import com.fitviet.app.domain.MonthlyPlanStatus
import com.fitviet.app.domain.MonthlyPlanTodayResolver
import com.fitviet.app.domain.MuscleGroup
import com.fitviet.app.domain.PlanPhase
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.domain.TodayPlanDayState
import com.fitviet.app.domain.TrainingGoal
import com.fitviet.app.domain.WorkoutCompositionCalculator
import com.fitviet.app.ui.workout.DEFAULT_REST_SECONDS
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** What the user actually picked on the Quick Generate screen (or its onboarding-prefilled
 * defaults) — deliberately simpler than [MonthlyPlanGenerationInput], which also carries data-layer
 * facts (catalog, PRs, recent volume/usage) this repository gathers itself so the UI layer never
 * has to know about [com.fitviet.app.data.local.dao.SetLogDao] or the exercise catalog. */
data class MonthlyPlanUserChoices(
    val goal: TrainingGoal,
    val level: ExerciseDifficulty,
    val splitTemplate: SplitTemplate,
    val daysPerWeek: Int,
    val totalWeeks: Int = 4,
    val customSplitDays: List<CustomSplitDay>? = null,
    val equipmentProfile: String? = null,
    val sessionDurationTargetMinutes: Int = 60,
    /** Redesign Gate 1d-ii — see [com.fitviet.app.domain.MonthlyPlanGenerationInput.restSeconds]'s
     * doc; no settings source exists for this yet, so every real call site leaves it at the
     * default. Known future gap (Phase 1 review): unlike [equipmentProfile], this value is NOT
     * persisted on [com.fitviet.app.data.local.entity.MonthlyPlanEntity], so [choicesFrom] can't
     * restore whatever value a plan was originally generated with — a regenerate always re-derives
     * the *current* default rather than the plan's own. Currently a no-op gap (every real path
     * uses the same hardcoded default, so there's nothing to drift from yet); revisit by adding a
     * persisted column once a real settings source exists. */
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val exclusionExerciseIds: Set<Long> = emptySet(),
    val excludedMuscleGroupCodes: Set<String> = emptySet(),
)

sealed interface RegenerateResult {
    data object Success : RegenerateResult

    /** The day/week/month has at least one day with a linked
     * [com.fitviet.app.data.local.entity.WorkoutSessionEntity] (completed or abandoned) — see the
     * "Hit & Run" plan's regenerate lock rule. For week/month regenerate, [Locked] still means
     * "every OTHER unlocked day was regenerated" (see [MonthlyPlanRepository.regenerateWeek]/
     * [regenerateMonth]'s doc) — it's a partial-success signal, not a full abort. */
    data object Locked : RegenerateResult
    data object NotFound : RegenerateResult
}

interface MonthlyPlanRepository {
    fun observeActivePlanId(): Flow<Long?>
    fun observePlan(planId: Long): Flow<MonthlyPlanEntity?>
    fun observeWeeksForPlan(planId: Long): Flow<List<MonthlyPlanWeekEntity>>
    fun observeDaysForPlan(planId: Long): Flow<List<MonthlyPlanDayEntity>>
    fun observeExercisesForDay(dayId: Long): Flow<List<MonthlyPlanExerciseEntity>>

    /** "Hit & Run" redesign (Gate 1c) — the single shared "what's today's plan status" resolution,
     * built entirely from this interface's own [observeActivePlanId]/[observeDaysForPlan]/
     * [observeExercisesForDay] (no new DAO dependency). Both [com.fitviet.app.data.repository
     * .DashboardRepository] (Today card) and [com.fitviet.app.ui.workout.WorkoutViewModel] (the
     * bare `workout` route's single entry point) call this instead of each resolving "today"
     * their own way — see [com.fitviet.app.domain.TodayMonthlyPlanCard]'s doc for why a shared,
     * exhaustive 5-case type replaced two separate ad hoc null-based resolutions. */
    fun observeTodaySession(today: LocalDate): Flow<TodayMonthlyPlanCard>

    /** "Hit & Run" (Gate 63+) Regenerate UI — every day in [planId] with at least one linked
     * [com.fitviet.app.data.local.entity.WorkoutSessionEntity], i.e. the same lock rule
     * [regenerateDay]/[regenerateWeek]/[regenerateMonth] enforce, exposed read-only so the UI can
     * show the guard *before* a tap rather than only reacting to a [RegenerateResult.Locked]. */
    fun observeLockedDayIds(planId: Long): Flow<Set<Long>>

    /** Reactive counterpart to [getDay] for the day-detail screen, and [observeIsDayLocked]'s
     * sibling — a live day row so a reschedule/regenerate performed elsewhere (or from this same
     * screen) is reflected without a manual refresh. */
    fun observeDay(dayId: Long): Flow<MonthlyPlanDayEntity?>

    /** Single-day reactive form of [observeLockedDayIds], for the day-detail screen (which only
     * ever needs one day's lock state, not a whole plan's). */
    fun observeIsDayLocked(dayId: Long): Flow<Boolean>

    /** One-shot lookup for [com.fitviet.app.ui.workout.MonthlyPlanDayWorkoutPlanner.resolveDay] —
     * that planner is given a specific day id already picked by the caller (e.g. via
     * [com.fitviet.app.domain.MonthlyPlanTodayResolver]), not a plan id to resolve "today" from
     * itself, so it needs the day row directly rather than the full plan's day list. */
    suspend fun getDay(dayId: Long): MonthlyPlanDayEntity?

    /** Generates a brand new plan and makes it the active one — any previously-active plan is
     * marked [MonthlyPlanStatus.SUPERSEDED] (never deleted: its completed days/sessions stay real
     * history), not replaced in place. Returns the new plan's id. */
    suspend fun generate(choices: MonthlyPlanUserChoices, today: LocalDate): Long

    /** Re-picks exercises for one day only — does not touch other weeks sharing the same session
     * label, since an explicit "I don't like this one" action on a single day isn't expected to
     * silently resync the rest of the block (see [com.fitviet.app.domain.MonthlyPlanGenerator.generateDayExercises]'s doc). */
    suspend fun regenerateDay(dayId: Long, today: LocalDate): RegenerateResult

    /** Regenerates every unlocked day in one week; a locked day is left byte-for-byte untouched
     * and doesn't abort the rest — the result is [RegenerateResult.Locked] only if EVERY day in
     * the week was locked (nothing at all changed), [Success] otherwise. */
    suspend fun regenerateWeek(weekId: Long, today: LocalDate): RegenerateResult

    /** Same "skip locked days individually" rule as [regenerateWeek], scoped to the whole plan. */
    suspend fun regenerateMonth(planId: Long, today: LocalDate): RegenerateResult

    /** Re-picks just the one exercise row, keeping its muscle group/tier — [avoidEquipment], when
     * non-null, additionally excludes that one equipment string for this pick only (the
     * "swap for equipment mismatch" action), without touching the plan's own [equipmentProfile]. */
    suspend fun swapExercise(monthlyPlanExerciseId: Long, avoidEquipment: String? = null): RegenerateResult

    /** [com.fitviet.app.domain.MissedWorkoutDetector] over the active plan's past days — read-only,
     * does not itself flip anything to MISSED (see [markMissed]). */
    suspend fun findMissedDays(planId: Long, today: LocalDate): List<MonthlyPlanDayEntity>

    /** Flips each day's status to MISSED — a detection marker so the next Dashboard load's
     * [findMissedDays] scan doesn't re-surface the same day, never touches [MonthlyPlanDayEntity.effectiveEpochDay]. */
    suspend fun markMissed(dayIds: List<Long>)

    /** Pushes a missed day onto today's slot for this cycle only (no cascade — see the plan's
     * judgment call #3): sets [MonthlyPlanDayEntity.effectiveEpochDay]/`dayOfWeek` to [today] and
     * status to RESCHEDULED. Whatever was originally scheduled for today is left as its own,
     * separate row (visible as its own missed/skipped entry) — this does not swap the two days. */
    suspend fun pushMissedDayToToday(dayId: Long, today: LocalDate): RegenerateResult

    /** Marks a missed day SKIPPED — [MonthlyPlanDayEntity.effectiveEpochDay] is preserved
     * unchanged (a historical "planned but skipped" record), nothing else about the plan changes. */
    suspend fun skipMissedDay(dayId: Long): RegenerateResult

    /** The minimal "manually rearrange" primitive: swaps two unlocked days' effectiveEpochDay/
     * dayOfWeek, both become RESCHEDULED. Refuses (returns [RegenerateResult.Locked]) if either
     * day is locked. */
    suspend fun swapTwoDays(dayIdA: Long, dayIdB: Long): RegenerateResult

    /** The PR-integration hook — call this instead of bare
     * [WorkoutRepository.completeSession] whenever the just-finished session came from a monthly
     * plan day. Bumps [MonthlyPlanExerciseEntity.targetWeightKg] on that exercise's future,
     * unlocked rows within the same plan when a logged set beats the generation-time PR by a real
     * margin — never touches completed/locked history. */
    suspend fun onMonthlyPlanSessionCompleted(monthlyPlanDayId: Long)
}

/** A fresh PR must beat what generation seeded by more than this to trigger a future-week bump —
 * avoids re-triggering on measurement/rounding noise (e.g. 40.0kg vs 40.5kg isn't "a new PR" for
 * planning purposes). */
private const val PR_BUMP_THRESHOLD_KG = 2.5

class RoomMonthlyPlanRepository(
    private val database: FitVietDatabase,
    private val monthlyPlanDao: MonthlyPlanDao,
    private val monthlyPlanWeekDao: MonthlyPlanWeekDao,
    private val monthlyPlanDayDao: MonthlyPlanDayDao,
    private val monthlyPlanExerciseDao: MonthlyPlanExerciseDao,
    private val splitTemplateDao: SplitTemplateDao,
    private val exerciseDao: ExerciseDao,
    private val setLogDao: SetLogDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val settingsDao: SettingsDao,
) : MonthlyPlanRepository {

    override fun observeActivePlanId(): Flow<Long?> = settingsDao.observe().map { it?.activeMonthlyPlanId }

    override fun observePlan(planId: Long): Flow<MonthlyPlanEntity?> = monthlyPlanDao.observeById(planId)

    override fun observeWeeksForPlan(planId: Long): Flow<List<MonthlyPlanWeekEntity>> = monthlyPlanWeekDao.observeForPlan(planId)

    override fun observeDaysForPlan(planId: Long): Flow<List<MonthlyPlanDayEntity>> = monthlyPlanDayDao.observeForPlan(planId)

    override fun observeLockedDayIds(planId: Long): Flow<Set<Long>> =
        workoutSessionDao.observeLockedMonthlyPlanDayIds(planId).map { it.toSet() }

    override fun observeDay(dayId: Long): Flow<MonthlyPlanDayEntity?> = monthlyPlanDayDao.observeById(dayId)

    override fun observeIsDayLocked(dayId: Long): Flow<Boolean> =
        workoutSessionDao.observeCountForMonthlyPlanDay(dayId).map { it > 0 }

    override fun observeExercisesForDay(dayId: Long): Flow<List<MonthlyPlanExerciseEntity>> = monthlyPlanExerciseDao.observeForDay(dayId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTodaySession(today: LocalDate): Flow<TodayMonthlyPlanCard> =
        observeActivePlanId().flatMapLatest { activePlanId ->
            if (activePlanId == null) {
                flowOf(TodayMonthlyPlanCard.NoPlan)
            } else {
                observeDaysForPlan(activePlanId).flatMapLatest { days ->
                    when (val state = MonthlyPlanTodayResolver.classify(activePlanId, days, today)) {
                        TodayPlanDayState.NoPlan -> flowOf(TodayMonthlyPlanCard.NoPlan)
                        TodayPlanDayState.PlanFinished -> flowOf(TodayMonthlyPlanCard.PlanFinished)
                        TodayPlanDayState.RestDay -> flowOf(TodayMonthlyPlanCard.RestDay)
                        is TodayPlanDayState.HasSession -> observeExercisesForDay(state.dayId).map { exercises ->
                            if (exercises.isEmpty()) {
                                TodayMonthlyPlanCard.Unavailable(dayId = state.dayId, sessionType = state.sessionType)
                            } else {
                                TodayMonthlyPlanCard.Training(
                                    dayId = state.dayId,
                                    sessionType = state.sessionType,
                                    exerciseCount = exercises.size,
                                    estimatedDurationMinutes = MonthlyPlanDayCardEstimator.estimateDurationMinutes(exercises),
                                )
                            }
                        }
                    }
                }
            }
        }

    override suspend fun getDay(dayId: Long): MonthlyPlanDayEntity? = monthlyPlanDayDao.getById(dayId)

    override suspend fun generate(choices: MonthlyPlanUserChoices, today: LocalDate): Long = database.withTransaction {
        val input = buildGenerationInput(choices, today)
        val draft = MonthlyPlanGenerator.generate(input)

        val settings = settingsDao.get() ?: SettingsEntity()
        settings.activeMonthlyPlanId?.let { oldId ->
            monthlyPlanDao.getById(oldId)?.let { old -> monthlyPlanDao.update(old.copy(status = MonthlyPlanStatus.SUPERSEDED.name)) }
        }

        val planId = monthlyPlanDao.insert(
            MonthlyPlanEntity(
                createdAtEpochMillis = System.currentTimeMillis(),
                startEpochDay = draft.startEpochDay,
                totalWeeks = draft.totalWeeks,
                goal = draft.goal.name,
                level = draft.level.name,
                splitTemplate = draft.splitTemplate.name,
                daysPerWeek = draft.daysPerWeek,
                sessionDurationTargetMinutes = draft.sessionDurationTargetMinutes,
                equipmentProfile = draft.equipmentProfile,
                exclusionExerciseIds = draft.exclusionExerciseIds.toList(),
                excludedMuscleGroupCodes = draft.excludedMuscleGroupCodes.toList(),
                customSplitTemplateId = if (draft.splitTemplate == SplitTemplate.CUSTOM) persistCustomSplit(choices.customSplitDays) else null,
                status = MonthlyPlanStatus.ACTIVE.name,
            ),
        )

        draft.weeks.forEach { week ->
            val weekId = monthlyPlanWeekDao.insert(MonthlyPlanWeekEntity(monthlyPlanId = planId, weekIndex = week.weekIndex, phase = week.phase.name))
            week.days.forEach { day ->
                val dayId = monthlyPlanDayDao.insert(
                    MonthlyPlanDayEntity(
                        monthlyPlanWeekId = weekId,
                        plannedEpochDay = day.epochDay,
                        effectiveEpochDay = day.epochDay,
                        dayOfWeek = day.dayOfWeek.value,
                        isRestDay = day.isRestDay,
                        sessionType = day.sessionType,
                        muscleGroupCodes = day.muscleGroupCodes,
                        status = MonthlyPlanDayStatus.SCHEDULED.name,
                    ),
                )
                if (day.exercises.isNotEmpty()) {
                    monthlyPlanExerciseDao.insertAll(day.exercises.map { it.toEntity(dayId) })
                }
            }
        }

        settingsDao.upsert(settings.copy(activeMonthlyPlanId = planId))
        planId
    }

    override suspend fun regenerateDay(dayId: Long, today: LocalDate): RegenerateResult = database.withTransaction {
        val day = monthlyPlanDayDao.getById(dayId) ?: return@withTransaction RegenerateResult.NotFound
        regenerateDayLocked(day, today)
    }

    override suspend fun regenerateWeek(weekId: Long, today: LocalDate): RegenerateResult = database.withTransaction {
        val days = monthlyPlanDayDao.getForWeek(weekId).filterNot { it.isRestDay }
        if (days.isEmpty()) return@withTransaction RegenerateResult.NotFound
        val results = days.map { regenerateDayLocked(it, today) }
        if (results.all { it == RegenerateResult.Locked }) RegenerateResult.Locked else RegenerateResult.Success
    }

    override suspend fun regenerateMonth(planId: Long, today: LocalDate): RegenerateResult = database.withTransaction {
        val weeks = monthlyPlanWeekDao.getForPlan(planId)
        if (weeks.isEmpty()) return@withTransaction RegenerateResult.NotFound
        val days = weeks.flatMap { monthlyPlanDayDao.getForWeek(it.id) }.filterNot { it.isRestDay }
        if (days.isEmpty()) return@withTransaction RegenerateResult.NotFound
        val results = days.map { regenerateDayLocked(it, today) }
        if (results.all { it == RegenerateResult.Locked }) RegenerateResult.Locked else RegenerateResult.Success
    }

    /** Shared by [regenerateDay]/[regenerateWeek]/[regenerateMonth] — all three already run inside
     * [database.withTransaction], so this assumes it's called from within one, not opening its own. */
    private suspend fun regenerateDayLocked(day: MonthlyPlanDayEntity, today: LocalDate): RegenerateResult {
        if (day.isRestDay || day.sessionType == null) return RegenerateResult.NotFound
        if (workoutSessionDao.countForMonthlyPlanDay(day.id) > 0) return RegenerateResult.Locked

        val week = monthlyPlanWeekDao.getById(day.monthlyPlanWeekId) ?: return RegenerateResult.NotFound
        val plan = monthlyPlanDao.getById(week.monthlyPlanId) ?: return RegenerateResult.NotFound
        val customDays = loadCustomSplitDays(plan)
        val slot = slotForDay(plan, customDays, day)
            ?: return RegenerateResult.NotFound

        val currentExerciseIds = monthlyPlanExerciseDao.getForDay(day.id).map { it.exerciseId }.toSet()
        val baseInput = buildGenerationInput(choicesFrom(plan, customDays), today)
        val input = baseInput.copy(exclusionExerciseIds = baseInput.exclusionExerciseIds + currentExerciseIds)
        val newExercises = MonthlyPlanGenerator.generateDayExercises(input, slot, PlanPhase.valueOf(week.phase), week.weekIndex)

        monthlyPlanExerciseDao.deleteForDay(day.id)
        if (newExercises.isNotEmpty()) monthlyPlanExerciseDao.insertAll(newExercises.map { it.toEntity(day.id) })
        return RegenerateResult.Success
    }

    override suspend fun swapExercise(monthlyPlanExerciseId: Long, avoidEquipment: String?): RegenerateResult = database.withTransaction {
        val target = monthlyPlanExerciseDao.getById(monthlyPlanExerciseId) ?: return@withTransaction RegenerateResult.NotFound
        val day = monthlyPlanDayDao.getById(target.monthlyPlanDayId) ?: return@withTransaction RegenerateResult.NotFound
        if (workoutSessionDao.countForMonthlyPlanDay(day.id) > 0) return@withTransaction RegenerateResult.Locked

        val week = monthlyPlanWeekDao.getById(day.monthlyPlanWeekId) ?: return@withTransaction RegenerateResult.NotFound
        val plan = monthlyPlanDao.getById(week.monthlyPlanId) ?: return@withTransaction RegenerateResult.NotFound
        val exercise = exerciseDao.getById(target.exerciseId) ?: return@withTransaction RegenerateResult.NotFound
        val group = MuscleGroup.entries.firstOrNull { it.name == exercise.muscleGroupCode }
            ?: return@withTransaction RegenerateResult.NotFound
        val customDays = loadCustomSplitDays(plan)
        val slot = slotForDay(plan, customDays, day)
            ?: return@withTransaction RegenerateResult.NotFound

        val siblingIds = monthlyPlanExerciseDao.getForDay(day.id).map { it.exerciseId }.toSet()
        val baseInput = buildGenerationInput(choicesFrom(plan, customDays), LocalDate.ofEpochDay(day.effectiveEpochDay))
        val equipmentExclusions = avoidEquipment
            ?.let { avoid -> baseInput.catalog.filter { it.equipment == avoid }.map { it.id }.toSet() }
            .orEmpty()
        val input = baseInput.copy(exclusionExerciseIds = baseInput.exclusionExerciseIds + siblingIds + equipmentExclusions)

        // Only the single (group, tier) slot this exercise occupies gets re-picked — narrowing the
        // slot to just its own muscle group keeps generateDayExercises() from also re-rolling this
        // day's other exercises.
        val replacement = MonthlyPlanGenerator.generateDayExercises(
            input,
            slot.copy(muscleGroups = setOf(group)),
            PlanPhase.valueOf(week.phase),
            week.weekIndex,
            onlyTier = com.fitviet.app.domain.ExerciseTier.valueOf(target.tier),
        ).singleOrNull() ?: return@withTransaction RegenerateResult.NotFound

        monthlyPlanExerciseDao.deleteAll(listOf(target))
        monthlyPlanExerciseDao.insertAll(listOf(replacement.toEntity(day.id).copy(orderIndex = target.orderIndex, tier = target.tier)))
        RegenerateResult.Success
    }

    override suspend fun findMissedDays(planId: Long, today: LocalDate): List<MonthlyPlanDayEntity> {
        val past = monthlyPlanDayDao.getPastNonRestDays(planId, today.toEpochDay())
        if (past.isEmpty()) return emptyList()
        val completedDayIds = past.filter { workoutSessionDao.countForMonthlyPlanDay(it.id) > 0 }.map { it.id }.toSet()
        return MissedWorkoutDetector.findMissed(past, completedDayIds)
    }

    override suspend fun markMissed(dayIds: List<Long>) = database.withTransaction {
        dayIds.forEach { id ->
            monthlyPlanDayDao.getById(id)?.let { day -> monthlyPlanDayDao.update(day.copy(status = MonthlyPlanDayStatus.MISSED.name)) }
        }
    }

    override suspend fun pushMissedDayToToday(dayId: Long, today: LocalDate): RegenerateResult = database.withTransaction {
        val day = monthlyPlanDayDao.getById(dayId) ?: return@withTransaction RegenerateResult.NotFound
        if (workoutSessionDao.countForMonthlyPlanDay(dayId) > 0) return@withTransaction RegenerateResult.Locked
        monthlyPlanDayDao.update(
            day.copy(effectiveEpochDay = today.toEpochDay(), dayOfWeek = today.dayOfWeek.value, status = MonthlyPlanDayStatus.RESCHEDULED.name),
        )
        RegenerateResult.Success
    }

    override suspend fun skipMissedDay(dayId: Long): RegenerateResult = database.withTransaction {
        val day = monthlyPlanDayDao.getById(dayId) ?: return@withTransaction RegenerateResult.NotFound
        monthlyPlanDayDao.update(day.copy(status = MonthlyPlanDayStatus.SKIPPED.name))
        RegenerateResult.Success
    }

    override suspend fun swapTwoDays(dayIdA: Long, dayIdB: Long): RegenerateResult = database.withTransaction {
        val dayA = monthlyPlanDayDao.getById(dayIdA) ?: return@withTransaction RegenerateResult.NotFound
        val dayB = monthlyPlanDayDao.getById(dayIdB) ?: return@withTransaction RegenerateResult.NotFound
        if (workoutSessionDao.countForMonthlyPlanDay(dayIdA) > 0 || workoutSessionDao.countForMonthlyPlanDay(dayIdB) > 0) {
            return@withTransaction RegenerateResult.Locked
        }
        monthlyPlanDayDao.update(
            dayA.copy(effectiveEpochDay = dayB.effectiveEpochDay, dayOfWeek = dayB.dayOfWeek, status = MonthlyPlanDayStatus.RESCHEDULED.name),
        )
        monthlyPlanDayDao.update(
            dayB.copy(effectiveEpochDay = dayA.effectiveEpochDay, dayOfWeek = dayA.dayOfWeek, status = MonthlyPlanDayStatus.RESCHEDULED.name),
        )
        RegenerateResult.Success
    }

    override suspend fun onMonthlyPlanSessionCompleted(monthlyPlanDayId: Long) = database.withTransaction {
        val day = monthlyPlanDayDao.getById(monthlyPlanDayId) ?: return@withTransaction
        val week = monthlyPlanWeekDao.getById(day.monthlyPlanWeekId) ?: return@withTransaction
        val completedExerciseIds = setLogDao.getCompletedExerciseIdsForMonthlyPlanDay(monthlyPlanDayId).toSet()
        val loggedExercises = monthlyPlanExerciseDao.getForDay(monthlyPlanDayId)
            .filter { it.exerciseId in completedExerciseIds }
        loggedExercises.forEach { logged ->
            val freshPr = setLogDao.getPersonalBest(logged.exerciseId)?.weightKg ?: return@forEach
            val seededAt = logged.targetWeightKg ?: 0.0
            if (freshPr - seededAt < PR_BUMP_THRESHOLD_KG) return@forEach

            val futureRows = monthlyPlanExerciseDao.getForPlanAndExercise(week.monthlyPlanId, logged.exerciseId)
                .filter { it.monthlyPlanDayId != monthlyPlanDayId }
            futureRows.forEach { row ->
                // "Future" = not yet locked by a session, regardless of week index — a day earlier
                // in the block that just hasn't been reached yet is exactly as eligible as a later
                // one; a day that already has a session (this one included) is never touched.
                if (workoutSessionDao.countForMonthlyPlanDay(row.monthlyPlanDayId) == 0) {
                    monthlyPlanExerciseDao.deleteAll(listOf(row))
                    monthlyPlanExerciseDao.insertAll(listOf(row.copy(targetWeightKg = freshPr)))
                }
            }
        }
    }

    // ---- shared helpers ----

    private suspend fun persistCustomSplit(customSplitDays: List<CustomSplitDay>?): Long? {
        if (customSplitDays.isNullOrEmpty()) return null
        return splitTemplateDao.insert(
            SplitTemplateEntity(
                userDefinedLabel = "Custom",
                daysPerWeek = customSplitDays.size,
                dayDefinitionsJson = CustomSplitCodec.encode(customSplitDays),
            ),
        )
    }

    private suspend fun loadCustomSplitDays(plan: MonthlyPlanEntity): List<CustomSplitDay>? {
        val templateId = plan.customSplitTemplateId ?: return null
        val template = splitTemplateDao.getById(templateId) ?: return null
        return CustomSplitCodec.decode(template.dayDefinitionsJson)
    }

    /** Custom slots may reuse a display label. Persisted muscle groups disambiguate them. */
    private fun slotForDay(
        plan: MonthlyPlanEntity,
        customSplitDays: List<CustomSplitDay>?,
        day: MonthlyPlanDayEntity,
    ) = if (SplitTemplate.valueOf(plan.splitTemplate) == SplitTemplate.CUSTOM) {
        val groups = day.muscleGroupCodes.mapNotNull { code ->
            MuscleGroup.entries.firstOrNull { it.name == code }
        }.toSet()
        day.sessionType?.let { label ->
            if (groups.isEmpty()) null else com.fitviet.app.domain.SessionTypeSlot(label, groups)
        }
    } else {
        day.sessionType?.let { label ->
            MonthlyPlanGenerator.slotFor(SplitTemplate.valueOf(plan.splitTemplate), customSplitDays, label)
        }
    }

    private fun choicesFrom(plan: MonthlyPlanEntity, customSplitDays: List<CustomSplitDay>?) = MonthlyPlanUserChoices(
        goal = TrainingGoal.valueOf(plan.goal),
        level = ExerciseDifficulty.valueOf(plan.level),
        splitTemplate = SplitTemplate.valueOf(plan.splitTemplate),
        daysPerWeek = plan.daysPerWeek,
        totalWeeks = plan.totalWeeks,
        customSplitDays = customSplitDays,
        equipmentProfile = plan.equipmentProfile,
        sessionDurationTargetMinutes = plan.sessionDurationTargetMinutes,
        exclusionExerciseIds = plan.exclusionExerciseIds.toSet(),
        excludedMuscleGroupCodes = plan.excludedMuscleGroupCodes.toSet(),
        // No persisted column for this yet (see MonthlyPlanUserChoices.restSeconds's doc) — a
        // regenerate always re-derives it at the current default, same as every other real call
        // site until a settings source exists.
    )

    /** Gathers everything [MonthlyPlanGenerator] needs beyond the user's own choices — the real
     * exercise catalog, personal bests, recent per-muscle-group volume (7-day window, feeds the
     * count formula's recovery adjustment), and recent per-exercise usage (4-week window, feeds
     * the variety soft-rank) — reusing [WorkoutCompositionCalculator]/[SetLogDao] exactly as
     * [com.fitviet.app.data.repository.DiaryRepository] already does for the same kind of data, so
     * this doesn't invent a second aggregation path. */
    private suspend fun buildGenerationInput(choices: MonthlyPlanUserChoices, today: LocalDate): MonthlyPlanGenerationInput {
        val zone = ZoneId.systemDefault()
        val catalog = exerciseDao.getAllOnce()

        val personalBests = catalog.mapNotNull { exercise ->
            setLogDao.getPersonalBest(exercise.id)?.weightKg?.let { exercise.id to it }
        }.toMap()

        val usageSinceMillis = today.minusDays(28).atStartOfDay(zone).toInstant().toEpochMilli()
        val recentExerciseUsage = setLogDao.observeRecentExerciseUsage(usageSinceMillis).first()
            .associate { it.exerciseId to Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }

        val breakdown = setLogDao.observeCompletedSetBreakdown().first()
        val completedSets = breakdown.map {
            CompletedSet(
                date = Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate(),
                muscleGroupCode = it.muscleGroupCode,
                movementType = it.movementType,
                volumeKg = it.weightKg * it.reps,
            )
        }
        val recentSetCountByMuscleGroup = WorkoutCompositionCalculator.muscleGroupWorkload(completedSets, today.minusDays(7))
            .associate { it.muscleGroup.name to it.setCount }

        return MonthlyPlanGenerationInput(
            goal = choices.goal,
            level = choices.level,
            splitTemplate = choices.splitTemplate,
            daysPerWeek = choices.daysPerWeek,
            totalWeeks = choices.totalWeeks,
            customSplitDays = choices.customSplitDays,
            equipmentProfile = choices.equipmentProfile,
            sessionDurationTargetMinutes = choices.sessionDurationTargetMinutes,
            restSeconds = choices.restSeconds,
            exclusionExerciseIds = choices.exclusionExerciseIds,
            excludedMuscleGroupCodes = choices.excludedMuscleGroupCodes,
            catalog = catalog,
            personalBests = personalBests,
            recentSetCountByMuscleGroup = recentSetCountByMuscleGroup,
            recentExerciseUsage = recentExerciseUsage,
            today = today,
        )
    }
}

private fun MonthlyPlanExerciseDraft.toEntity(dayId: Long) = MonthlyPlanExerciseEntity(
    monthlyPlanDayId = dayId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    tier = tier.name,
    selectionReasonCode = selectionReasonCode,
    targetSets = targetSets,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetWeightKg = targetWeightKg,
    supersetGroup = supersetGroup,
)
