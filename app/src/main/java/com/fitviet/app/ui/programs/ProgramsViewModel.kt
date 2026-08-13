package com.fitviet.app.ui.programs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.R
import com.fitviet.app.data.local.entity.ExerciseEntity
import com.fitviet.app.data.local.entity.MonthlyPlanDayEntity
import com.fitviet.app.data.local.entity.MonthlyPlanEntity
import com.fitviet.app.data.local.entity.MonthlyPlanWeekEntity
import com.fitviet.app.data.local.entity.ProgramEntity
import com.fitviet.app.data.repository.ExerciseRepository
import com.fitviet.app.data.repository.ImportProgramResult
import com.fitviet.app.data.repository.MonthlyPlanRepository
import com.fitviet.app.data.repository.MonthlyPlanUserChoices
import com.fitviet.app.data.repository.OnboardingRepository
import com.fitviet.app.data.repository.ProgramRepository
import com.fitviet.app.domain.ExerciseDifficulty
import com.fitviet.app.domain.MonthlyPlanProgress
import com.fitviet.app.domain.MonthlyPlanProgressSummary
import com.fitviet.app.domain.ProgramDifficulty
import com.fitviet.app.domain.SplitTemplate
import com.fitviet.app.domain.TodayMonthlyPlanCard
import com.fitviet.app.domain.TrainingGoal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Filter chip tag; null means "Tất cả" (no tag filter). */
data class ProgramFilter(@StringRes val labelRes: Int, val tag: String?)

val PROGRAM_FILTERS = listOf(
    ProgramFilter(R.string.programs_filter_all, null),
    ProgramFilter(R.string.programs_filter_muscle_gain, "Tăng cơ"),
    ProgramFilter(R.string.programs_filter_fat_loss, "Giảm mỡ"),
    ProgramFilter(R.string.programs_filter_home, "Tại nhà"),
    ProgramFilter(R.string.programs_filter_gym, "Phòng gym"),
)

data class ProgramsUiState(
    val searchQuery: String = "",
    val selectedFilterIndex: Int = 0,
    val visiblePrograms: List<ProgramEntity> = emptyList(),
    /** Only populated while actively searching — the placeholder text says "Tìm giáo án, bài tập…". */
    val matchingExercises: List<ExerciseEntity> = emptyList(),
    /** Redesign Gate 2b — whether generating from a program card would replace an already-active
     * monthly plan, so the screen can gate the destructive action behind a confirm dialog (see
     * `ProgramsListScreen`'s own doc) instead of a bare tap silently discarding it. */
    val hasActivePlan: Boolean = false,
    val isGenerating: Boolean = false,
    /** Redesign Phase 3a — the Kế hoạch tab's progress/today/upcoming-days card cluster. Null with
     * no active plan (the tab falls back to its empty-state CTA instead). */
    val activePlan: ActivePlanUi? = null,
)

/** Redesign Phase 3a. */
data class ActivePlanUi(
    /** Real value for the mock's literal "PLAN THÁNG 8" — this app has no separate plan name, so
     * the calendar month the plan was created in (1-12, local timezone) is the only real
     * substitute. */
    val creationMonth: Int,
    val splitTemplate: SplitTemplate,
    val totalWeeks: Int,
    val progress: MonthlyPlanProgressSummary,
    /** Reused as-is from [com.fitviet.app.data.repository.MonthlyPlanRepository.observeTodaySession]
     * — the exact same resolution Dashboard's Today card and the bare `workout` route already
     * share, so the Kế hoạch tab never disagrees with either about what "today" means. */
    val todayCard: TodayMonthlyPlanCard,
    /** "NGÀY N/T" — null together with [todayCard] not carrying a `dayId` (RestDay/PlanFinished/
     * NoPlan). */
    val dayOfPlan: Int?,
    /** The next 2 upcoming (not-yet-reached) days, for a short preview — the full month lives one
     * drill-in away at [com.fitviet.app.ui.monthlyplan.MonthlyPlanDetailScreen]. */
    val upcomingDays: List<UpcomingDayUi>,
)

data class UpcomingDayUi(
    val dayId: Long,
    val dayOfPlan: Int,
    val dayOfWeek: DayOfWeek,
    val isRestDay: Boolean,
    val sessionType: String?,
)

/** Output of [ProgramsViewModel]'s first 5-source `combine{}` — chained with [ProgramsViewModel
 * .isGenerating] via a 6th, separate `.combine()` since kotlinx.coroutines has no typed
 * `combine{}` overload past 5 flows (same idiom as [com.fitviet.app.data.repository
 * .DashboardRepository]'s own `Stage1Data`). */
private data class ProgramsStage1(
    val programs: List<ProgramEntity>,
    val query: String,
    val filterIndex: Int,
    val exercises: List<ExerciseEntity>,
    val activePlanId: Long?,
)

/** Redesign Phase 3a — the active plan's own raw data, joined via a sibling flow to
 * [ProgramsStage1] rather than nested inside its `combine{}` — nesting a second
 * `observeActivePlanId().flatMapLatest{}` inside the block that runs per [ProgramsStage1] emission
 * would re-subscribe to the plan/day-list query on every search-query/filter keystroke, not just on
 * a real plan-id change (the exact anti-pattern the Phase 2 checkpoint review's M4 finding caught
 * in [com.fitviet.app.data.repository.DashboardRepository]). Null with no active plan. */
private data class PlanStage(
    val plan: MonthlyPlanEntity,
    val weeks: List<MonthlyPlanWeekEntity>,
    val days: List<MonthlyPlanDayEntity>,
    val completedDayIds: Set<Long>,
    val todayCard: TodayMonthlyPlanCard,
)

class ProgramsViewModel(
    private val programRepository: ProgramRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
    private val onboardingRepository: OnboardingRepository,
    exerciseRepository: ExerciseRepository,
    private val databaseReady: Deferred<Unit>,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedFilterIndex = MutableStateFlow(0)
    private val isGenerating = MutableStateFlow(false)

    // Awaits seeding first — a one-shot read taken before the async seed transaction commits
    // would otherwise emit an empty exercise list once and never refresh (unlike the programs
    // Flow above, which is Room-observed and updates on its own).
    private val allExercises = flow {
        databaseReady.await()
        emit(exerciseRepository.getAll())
    }

    // Recomputed once per ViewModel instance rather than a full day-ticker (see
    // com.fitviet.app.data.repository.DayTicker) — this tab isn't a long-lived background
    // subscription the way Dashboard is, so the small risk of "today" going stale across a
    // midnight rollover while the tab happens to stay open is an accepted, deliberate scope call,
    // same precedent as WorkoutViewModel.resolveTodaySessionAndStart's own `LocalDate.now()`.
    private val today = LocalDate.now()

    // Gate 3a review — observeActivePlanId() re-runs on ANY settings-row write (profile edits,
    // onboarding changes, the superset-hint flag, lastCelebratedStreakDays, ...), not just an
    // actual plan-id change, since it has no distinctUntilChanged of its own. Applied here so an
    // unrelated settings write can't tear down and re-subscribe planFlow's 5 inner queries.
    private val activePlanIdFlow = monthlyPlanRepository.observeActivePlanId().distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val planFlow = activePlanIdFlow.flatMapLatest { planId ->
        if (planId == null) {
            flowOf(null)
        } else {
            combine(
                monthlyPlanRepository.observePlan(planId),
                monthlyPlanRepository.observeWeeksForPlan(planId),
                monthlyPlanRepository.observeDaysForPlan(planId),
                monthlyPlanRepository.observeCompletedDayIds(planId),
                monthlyPlanRepository.observeTodaySession(today),
            ) { plan, weeks, days, completedDayIds, todayCard ->
                plan?.let { PlanStage(it, weeks, days, completedDayIds, todayCard) }
            }
        }
    }
        // Decouples uiState's first emission from planFlow's own (potentially several-query)
        // first emission, so the search/filter/program-list part of the screen (which doesn't
        // read `activePlan`) isn't held back by monthly-plan data it doesn't need. Traded off
        // against a warm re-subscribe (returning to the tab after WhileSubscribed's 5s window)
        // briefly showing activePlan = null before planFlow's real value lands — cosmetic, and
        // the cold-start gain this buys is likely dwarfed by allExercises' own databaseReady
        // wait anyway, so this is left as a minor, self-correcting flicker rather than solved
        // properly (e.g. caching the last non-null PlanStage across resubscribes).
        .onStart { emit(null) }

    val uiState: StateFlow<ProgramsUiState> = combine(
        programRepository.observeAll(),
        searchQuery,
        selectedFilterIndex,
        allExercises,
        activePlanIdFlow,
    ) { programs, query, filterIndex, exercises, activePlanId ->
        ProgramsStage1(programs, query, filterIndex, exercises, activePlanId)
    }.combine(planFlow) { stage1, planStage -> stage1 to planStage }
        .combine(isGenerating) { (stage1, planStage), generating ->
            val tag = PROGRAM_FILTERS[stage1.filterIndex].tag
            val visiblePrograms = stage1.programs.filter { program ->
                (tag == null || program.tags.contains(tag)) &&
                    (stage1.query.isBlank() || program.titleVi.contains(stage1.query, ignoreCase = true))
            }
            val matchingExercises = if (stage1.query.isBlank()) {
                emptyList()
            } else {
                stage1.exercises.filter { it.nameVi.contains(stage1.query, ignoreCase = true) }
            }
            ProgramsUiState(
                searchQuery = stage1.query,
                selectedFilterIndex = stage1.filterIndex,
                visiblePrograms = visiblePrograms,
                matchingExercises = matchingExercises,
                hasActivePlan = stage1.activePlanId != null,
                isGenerating = generating,
                activePlan = planStage?.let(::toActivePlanUi),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramsUiState())

    private fun toActivePlanUi(stage: PlanStage): ActivePlanUi {
        val resolvedTodayDayId = when (val card = stage.todayCard) {
            is TodayMonthlyPlanCard.Training -> card.dayId
            is TodayMonthlyPlanCard.Unavailable -> card.dayId
            is TodayMonthlyPlanCard.Completed -> card.dayId
            else -> null
        }
        val upcomingDays = stage.days
            .filter { it.effectiveEpochDay > today.toEpochDay() }
            .sortedBy { it.effectiveEpochDay }
            .take(2)
            .map { d ->
                UpcomingDayUi(
                    dayId = d.id,
                    dayOfPlan = MonthlyPlanProgress.dayOfPlan(stage.days, d.id) ?: 0,
                    dayOfWeek = LocalDate.ofEpochDay(d.effectiveEpochDay).dayOfWeek,
                    isRestDay = d.isRestDay,
                    sessionType = d.sessionType,
                )
            }
        return ActivePlanUi(
            creationMonth = Instant.ofEpochMilli(stage.plan.createdAtEpochMillis).atZone(ZoneId.systemDefault()).monthValue,
            splitTemplate = SplitTemplate.entries.find { it.name == stage.plan.splitTemplate } ?: SplitTemplate.FULL_BODY,
            totalWeeks = stage.plan.totalWeeks,
            progress = MonthlyPlanProgress.summarize(stage.weeks, stage.days, stage.completedDayIds, today),
            todayCard = stage.todayCard,
            dayOfPlan = resolvedTodayDayId?.let { MonthlyPlanProgress.dayOfPlan(stage.days, it) },
            upcomingDays = upcomingDays,
        )
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(index: Int) {
        selectedFilterIndex.value = index
    }

    /** Feature #1 — parses [json] (read from a user-picked file) as a FitViet program export and
     * inserts it as a new program if valid. See [ProgramRepository.importProgram]. */
    suspend fun importProgram(json: String): ImportProgramResult = programRepository.importProgram(json)

    /** Feature #1, redesign Gate 2b — the JSON text to share for [programId] via the Android share
     * sheet (moved here from the retired Weekly Schedule screen; see [ProgramRepository
     * .exportProgram]'s own doc for the null case). */
    suspend fun exportProgram(programId: Long): String? = programRepository.exportProgram(programId)

    /** Redesign Gate 2b — [program]'s primary "commit" action: generates a real
     * [com.fitviet.app.data.local.entity.MonthlyPlanEntity] from its own
     * [ProgramEntity.goal]/[ProgramEntity.splitTemplate]/[ProgramEntity.sessionsPerWeek]/[ProgramEntity.level]
     * rather than running the program's own day/exercise rows directly (see [ProgramEntity]'s own
     * doc — this is what makes it "read-only generation input"). Same await-then-navigate/
     * double-tap-guard pattern as [com.fitviet.app.ui.quickgenerate.QuickGenerateViewModel.generate]/
     * [com.fitviet.app.ui.onboarding.OnboardingViewModel.submit]. [ProgramEntity.sessionsPerWeek]
     * is coerced into the generator's supported 2..6 range — an imported program's file only
     * validates it as `> 0`, so an out-of-range value (1, or 7+) would otherwise throw out of
     * `MonthlyPlanGenerator.generate` (`require(daysPerWeek in SPACING_TABLE)`) instead of
     * silently generating a slightly different cadence than the card advertised. `equipmentProfile`
     * reads the user's own onboarding answer (same source [com.fitviet.app.ui.quickgenerate
     * .QuickGenerateViewModel] prefills from), not [program.equipment] — a home-only user tapping
     * a gym program would otherwise get a plan full of equipment they don't have; onboarding's
     * answer is the actual constraint that matters here, the card is just choosing goal/split/
     * cadence. [MonthlyPlanUserChoices.totalWeeks] stays at its 4-week default regardless of
     * [program.durationWeeks] (the generator only supports 4 or 5) — a known, pre-existing
     * generator limitation this gate doesn't expand, not something worth fixing at this layer. */
    suspend fun generateFromProgram(program: ProgramEntity): Boolean {
        databaseReady.await()
        if (isGenerating.value) return false
        isGenerating.value = true
        return try {
            val equipmentProfile = onboardingRepository.getSelections().equipmentProfile
            monthlyPlanRepository.generate(
                MonthlyPlanUserChoices(
                    goal = TrainingGoal.entries.find { it.name == program.goal } ?: TrainingGoal.HYPERTROPHY,
                    level = ProgramDifficulty.exerciseDifficultyFor(program.level),
                    splitTemplate = SplitTemplate.entries.find { it.name == program.splitTemplate } ?: SplitTemplate.FULL_BODY,
                    daysPerWeek = program.sessionsPerWeek.coerceIn(MIN_GENERATOR_DAYS_PER_WEEK, MAX_GENERATOR_DAYS_PER_WEEK),
                    equipmentProfile = equipmentProfile,
                ),
                LocalDate.now(),
            )
            true
        } finally {
            isGenerating.value = false
        }
    }

    class Factory(
        private val programRepository: ProgramRepository,
        private val monthlyPlanRepository: MonthlyPlanRepository,
        private val onboardingRepository: OnboardingRepository,
        private val exerciseRepository: ExerciseRepository,
        private val databaseReady: Deferred<Unit>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProgramsViewModel(programRepository, monthlyPlanRepository, onboardingRepository, exerciseRepository, databaseReady) as T
    }
}

private const val MIN_GENERATOR_DAYS_PER_WEEK = 2
private const val MAX_GENERATOR_DAYS_PER_WEEK = 6
