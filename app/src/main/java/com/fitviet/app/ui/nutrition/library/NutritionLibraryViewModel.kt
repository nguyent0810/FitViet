package com.fitviet.app.ui.nutrition.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.MeasurementDao
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.data.local.entity.UserMealPlanEntity
import com.fitviet.app.data.repository.MealPlanOperationResult
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.data.repository.MealPlanUserWizardChoices
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.data.repository.RecipeFilter
import com.fitviet.app.data.repository.RecipeRepository
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.NutritionGoals
import com.fitviet.app.domain.NutritionTargetsCalculator
import com.fitviet.app.domain.RecipeWithNutrition
import com.fitviet.app.util.normalizeVietnamese
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Redesign Gate 5b-i/5b-ii — all three tabs the mock's own "4 màn cũ → 3 tab con" footer note
 * describes. [PLAN] landed one gate after [RECIPES]/[TEMPLATES], per the Phase 5 plan-check's own
 * "present-but-dead is worse than absent for one gate" call — it was deliberately absent as a pill
 * until this gate, not disabled. */
enum class LibraryTab { RECIPES, TEMPLATES, PLAN }

/** How many recipes the Món ăn tab shows when browsing (no active search) — matches the mock's
 * own literal 3-suggestion example closely enough while leaving a little headroom; capped rather
 * than exact since [rankForGoal] is a heuristic, not a hand-curated list. */
private const val SUGGESTED_RECIPE_COUNT = 5

/** Review finding (Gate 5b-i) — the tab's own header claims "GỢI Ý CHO MỤC TIÊU" (suggested for
 * your goal), so the list needs an actual goal-driven order, not just the header text. No
 * dedicated nutrient-density field exists on [RecipeWithNutrition] for this — [BULK] approximates
 * "high protein" via protein grams per kcal (a real, if rough, density signal); [CUT] approximates
 * "light" via raw kcal ascending. [MAINTAIN] has no comparably obvious single axis, so it's left in
 * whatever order [RecipeRepository.observeRecipes] returned rather than inventing one. */
private fun rankForGoal(recipes: List<RecipeWithNutrition>, goal: NutritionGoal): List<RecipeWithNutrition> = when (goal) {
    NutritionGoal.BULK -> recipes.sortedByDescending { it.standardTotals.proteinG.toDouble() / it.standardTotals.kcal.coerceAtLeast(1) }
    NutritionGoal.CUT -> recipes.sortedBy { it.standardTotals.kcal }
    NutritionGoal.MAINTAIN -> recipes
}

/** The create-plan wizard's own editable fields — same shape as the retired `CreatePlanViewModel`'s
 * `CreatePlanUiState`, just without its `isGenerating` (lives in [LibraryUiState] directly,
 * alongside the other two tabs' own action flags). `CreatePlanUiState.isLoading` has no equivalent
 * here — the old screen declared it but never actually read it, so nothing was lost. */
data class PlanWizardState(
    val goal: NutritionGoal = NutritionGoal.MAINTAIN,
    val kcalTarget: Int = 2000,
    val proteinTargetG: Int = 130,
    val mealsPerDay: Int = 3,
    /** `null` = no ceiling — matches [MealPlanUserWizardChoices.cookTimeCeilingMinutes]'s own
     * contract 1:1. */
    val cookTimeCeilingMinutes: Int? = null,
)

/** The Kế hoạch tuần tab's "manage" mode summary — [UserMealPlanEntity]'s own wizard-choice fields,
 * read back for display once a plan already exists. */
data class ActivePlanSummary(
    val planId: Long,
    val goal: NutritionGoal,
    val kcalTarget: Int,
    val proteinTargetG: Int,
    val mealsPerDay: Int,
)

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.RECIPES,
    val searchQuery: String = "",
    val recipes: List<RecipeWithNutrition> = emptyList(),
    /** Judgment Call A from the Phase 5 plan-check — the retired `FoodsScreen`'s raw-ingredient
     * list has no tab of its own in the mock's 3-tab spec, so it folds into the Recipes tab as a
     * secondary, search-filtered section rather than being dropped outright. */
    val foods: List<FoodEntity> = emptyList(),
    val templates: List<MealPlanTemplateEntity> = emptyList(),
    val matchedGoal: NutritionGoal = NutritionGoal.MAINTAIN,
    val matchedTemplate: MealPlanTemplateEntity? = null,
    val kcalRemainingToday: Int = 0,
    val isApplyingTemplate: Boolean = false,
    /** Judgment Call B from the Phase 5 plan-check — the Kế hoạch tuần tab is state-dependent, not
     * a fixed wizard: [activePlanSummary] non-null means "manage" mode (summary + regenerate +
     * link into `nutrition/plan`); null means "create" mode ([wizard] fields, editable). */
    val activePlanSummary: ActivePlanSummary? = null,
    val wizard: PlanWizardState = PlanWizardState(),
    val isGeneratingPlan: Boolean = false,
    val isRegeneratingPlan: Boolean = false,
)

/** Merges what used to be `DiscoverViewModel`/`FoodsViewModel`/`TemplatesViewModel`/
 * `CreatePlanViewModel` into one VM with tab-scoped state — the plan-checked precedent for this
 * merge is [com.fitviet.app.ui.handbook.HandbookViewModel] (one VM, `selectedTab` in state, two
 * unrelated data sources) and [com.fitviet.app.ui.programs.ProgramsViewModel]'s own multi-source
 * staging (`ProgramsStage1`), not four surviving child VMs — the reviewer's own ruling was that
 * Gate 3c's `QuickGenerateViewModel` (which DID stay separate) doesn't apply here since that sheet
 * has multiple unrelated host screens where this screen's tabs have exactly one host. Past 5
 * source flows there's no typed `combine{}` overload, so this follows
 * [com.fitviet.app.ui.programs.ProgramsViewModel]'s own staged-combine idiom rather than nesting. */
class NutritionLibraryViewModel(
    private val recipeRepository: RecipeRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val settingsDao: SettingsDao,
    private val nutritionRepository: NutritionRepository,
    private val measurementDao: MeasurementDao,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedTab = MutableStateFlow(LibraryTab.RECIPES)
    private val isApplyingTemplate = MutableStateFlow(false)
    private val isGeneratingPlan = MutableStateFlow(false)
    private val isRegeneratingPlan = MutableStateFlow(false)
    private val wizardState = MutableStateFlow(PlanWizardState())

    // Same "prefilled but freely editable" idiom the retired `CreatePlanViewModel` established
    // (itself following `QuickGenerateViewModel`'s own precedent, including the exact fix for the
    // CTA-swallows-first-tap bug: `initialization.join()` before reading state in generatePlan()).
    // Review finding (Gate 5b-ii) — `.update { it.copy(...) }`, not a wholesale `wizardState.value =
    // PlanWizardState(...)`: the latter would silently discard `mealsPerDay`/`cookTimeCeilingMinutes`
    // (or any other field) if the user reached the PLAN tab and tapped a chip before these two Room
    // reads resolved — a narrow window, but a real one, and `copy` closes it for free.
    private val wizardInitialization: Job = viewModelScope.launch {
        val settings = settingsDao.get()
        val weightKg = measurementDao.observeLatest().first()?.weightKg ?: DEFAULT_BODY_WEIGHT_KG
        val goal = NutritionGoal.fromStored(settings?.selectedGoal)
        wizardState.update {
            it.copy(
                goal = goal,
                kcalTarget = NutritionTargetsCalculator.defaultKcalTarget(weightKg, goal),
                proteinTargetG = NutritionTargetsCalculator.defaultProteinTargetG(weightKg),
            )
        }
    }

    // Query travels alongside its own results (rather than a separate combine input) so a
    // fast-typing user always sees a query/result pair that agrees, matching the retired
    // `DiscoverViewModel`'s own precedent for the same reason.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val recipesFlow: Flow<Pair<String, List<RecipeWithNutrition>>> = searchQuery.flatMapLatest { query ->
        val normalized = query.takeIf { it.isNotBlank() }?.let(::normalizeVietnamese)
        recipeRepository.observeRecipes(RecipeFilter(searchQuery = normalized)).map { query to it }
    }

    // Unfiltered — [selectedTab]/[searchQuery]'s effect on foods (dimming/matching) is applied in
    // the final transform below, same "filter client-side, never re-subscribe" idiom the retired
    // `FoodsViewModel` already used for its own category dimming.
    private val foodsFlow: Flow<List<FoodEntity>> = recipeRepository.observeFoods(null)

    private data class LibraryStage1(
        val query: String,
        val recipes: List<RecipeWithNutrition>,
        val tab: LibraryTab,
        val foods: List<FoodEntity>,
        val templates: List<MealPlanTemplateEntity>,
        val goal: NutritionGoal,
    )

    private val stage1: Flow<LibraryStage1> = combine(
        recipesFlow,
        selectedTab,
        foodsFlow,
        mealPlanRepository.observeTemplates(),
        settingsDao.observe(),
    ) { (query, recipes), tab, foods, templates, settings ->
        LibraryStage1(
            query = query,
            recipes = recipes,
            tab = tab,
            foods = foods,
            templates = templates,
            goal = NutritionGoal.fromStored(settings?.selectedGoal),
        )
    }

    private data class ActionFlags(val applyingTemplate: Boolean, val generatingPlan: Boolean, val regeneratingPlan: Boolean)

    private val actionFlags: Flow<ActionFlags> =
        combine(isApplyingTemplate, isGeneratingPlan, isRegeneratingPlan, ::ActionFlags)

    private data class LibraryStage2(
        val stage1: LibraryStage1,
        val kcalToday: Int,
        val activePlan: UserMealPlanEntity?,
        val wizard: PlanWizardState,
    )

    private val stage2: Flow<LibraryStage2> = stage1
        .combine(nutritionRepository.observe()) { stage, nutritionData -> stage to nutritionData.totals.kcal }
        .combine(mealPlanRepository.observeActivePlan()) { (stage, kcalToday), activePlan -> Triple(stage, kcalToday, activePlan) }
        .combine(wizardState) { (stage, kcalToday, activePlan), wizard -> LibraryStage2(stage, kcalToday, activePlan, wizard) }

    val uiState: StateFlow<LibraryUiState> = stage2
        .combine(actionFlags) { (stage, kcalToday, activePlan, wizard), flags ->
            val normalizedQuery = stage.query.takeIf { it.isNotBlank() }?.let(::normalizeVietnamese)
            val isSearching = normalizedQuery != null
            // Review finding (Gate 5b-i) — with no search active, `recipesFlow` returns the WHOLE
            // catalog, which both contradicted the "GỢI Ý CHO MỤC TIÊU" (suggested for your goal)
            // header and buried the one-tap template promo card and the folded-in foods section
            // dozens of scroll positions down. Ranked + capped when browsing; once the user
            // searches, the server-side [RecipeFilter.searchQuery] result is shown in full instead
            // (a search should never hide a real match just to keep the list short).
            val suggestedRecipes = if (isSearching) stage.recipes else rankForGoal(stage.recipes, stage.goal).take(SUGGESTED_RECIPE_COUNT)
            // Foods (Judgment Call A) and the template promo card both read as noise ahead of a
            // real query — 37 raw ingredients under 25 ranked recipes, or a plan-replacement CTA
            // sitting inside search results that don't match it — so both are search-gated per the
            // same review finding.
            val matchingFoods = if (!isSearching) {
                emptyList()
            } else {
                stage.foods.filter { it.normalizedName.contains(normalizedQuery) }
            }
            val matchingTemplates = if (!isSearching) {
                stage.templates
            } else {
                stage.templates.filter { normalizeVietnamese(it.nameVi).contains(normalizedQuery) || normalizeVietnamese(it.descriptionVi).contains(normalizedQuery) }
            }
            LibraryUiState(
                selectedTab = stage.tab,
                searchQuery = stage.query,
                recipes = suggestedRecipes,
                foods = matchingFoods,
                templates = matchingTemplates,
                matchedGoal = stage.goal,
                matchedTemplate = if (isSearching) null else stage.templates.firstOrNull { it.goalCode == stage.goal.name },
                kcalRemainingToday = (NutritionGoals.KCAL - kcalToday).coerceAtLeast(0),
                isApplyingTemplate = flags.applyingTemplate,
                activePlanSummary = activePlan?.let {
                    ActivePlanSummary(
                        planId = it.id,
                        goal = NutritionGoal.fromStored(it.goalCode),
                        kcalTarget = it.kcalTarget,
                        proteinTargetG = it.proteinTargetG,
                        mealsPerDay = it.mealsPerDay,
                    )
                },
                wizard = wizard,
                isGeneratingPlan = flags.generatingPlan,
                isRegeneratingPlan = flags.regeneratingPlan,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun selectTab(tab: LibraryTab) {
        selectedTab.value = tab
    }

    /** Mock's own literal "+ Thêm" action on each Món ăn row — logs the recipe as eaten today via
     * the same [NutritionRepository] the Home screen's own "+ Thêm món" preset-adder uses. No slot
     * choice here (single implicit "Bữa phụ" default, per [NutritionRepository.addMeal]'s own
     * default) — the mock's dual "Bữa phụ"/"+ Thêm vào bữa trưa" choice only exists on the Recipe
     * Detail screen (Gate 5c), a different action from a different surface. */
    fun addToToday(recipe: RecipeWithNutrition) {
        viewModelScope.launch { nutritionRepository.addMeal(recipe) }
    }

    /** Same re-entrant-tap guard as the retired `TemplatesViewModel.useTemplate` (the Quick
     * Generate CTA bug precedent) — kept as an identical copy rather than a shared helper since
     * the flags below have no other relationship. */
    suspend fun useTemplate(templateId: Long): Boolean {
        if (isApplyingTemplate.value) return false
        isApplyingTemplate.value = true
        return try {
            mealPlanRepository.generateFromTemplate(templateId)
            true
        } finally {
            isApplyingTemplate.value = false
        }
    }

    fun selectWizardGoal(goal: NutritionGoal) = wizardState.update { it.copy(goal = goal) }
    fun incrementWizardKcal() = wizardState.update { it.copy(kcalTarget = (it.kcalTarget + KCAL_STEP).coerceAtMost(MAX_KCAL)) }
    fun decrementWizardKcal() = wizardState.update { it.copy(kcalTarget = (it.kcalTarget - KCAL_STEP).coerceAtLeast(MIN_KCAL)) }
    fun incrementWizardProtein() = wizardState.update { it.copy(proteinTargetG = (it.proteinTargetG + PROTEIN_STEP_G).coerceAtMost(MAX_PROTEIN_G)) }
    fun decrementWizardProtein() = wizardState.update { it.copy(proteinTargetG = (it.proteinTargetG - PROTEIN_STEP_G).coerceAtLeast(MIN_PROTEIN_G)) }
    fun selectWizardMealsPerDay(count: Int) = wizardState.update { it.copy(mealsPerDay = count) }
    fun selectWizardCookTimeCeiling(minutes: Int?) = wizardState.update { it.copy(cookTimeCeilingMinutes = minutes) }

    /** Awaited by the caller so a screen-pop right after can't cancel the generation
     * mid-transaction — same contract as the retired `CreatePlanViewModel.generate`. */
    suspend fun generatePlan(): Boolean {
        wizardInitialization.join()
        if (isGeneratingPlan.value) return false
        isGeneratingPlan.value = true
        val state = wizardState.value
        return try {
            mealPlanRepository.generateFromWizard(
                MealPlanUserWizardChoices(
                    goal = state.goal,
                    kcalTarget = state.kcalTarget,
                    proteinTargetG = state.proteinTargetG,
                    mealsPerDay = state.mealsPerDay,
                    cookTimeCeilingMinutes = state.cookTimeCeilingMinutes,
                ),
            )
            true
        } finally {
            isGeneratingPlan.value = false
        }
    }

    /** No confirmation dialog before replacing the active week — matches `PlanScreen`'s own
     * existing "Tạo lại tuần" link exactly (unlike the workout side's `ProgramsListScreen`, which
     * does confirm); this is a re-skin of an existing action's entry point, not a new judgment
     * call about whether that action should be gated. */
    suspend fun regenerateActivePlan(planId: Long): Boolean {
        if (isRegeneratingPlan.value) return false
        isRegeneratingPlan.value = true
        return try {
            mealPlanRepository.regenerateWeek(planId) is MealPlanOperationResult.Success
        } finally {
            isRegeneratingPlan.value = false
        }
    }

    class Factory(
        private val recipeRepository: RecipeRepository,
        private val mealPlanRepository: MealPlanRepository,
        private val settingsDao: SettingsDao,
        private val nutritionRepository: NutritionRepository,
        private val measurementDao: MeasurementDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NutritionLibraryViewModel(recipeRepository, mealPlanRepository, settingsDao, nutritionRepository, measurementDao) as T
    }

    companion object {
        /** No [com.fitviet.app.data.local.entity.MeasurementEntity] logged yet — a plausible
         * adult mid-range fallback so the kcal/protein defaults are still sane numbers instead of
         * zero, exactly the same "slider pre-fill, not a data-integrity concern" spirit
         * [NutritionTargetsCalculator]'s own doc comment already describes. */
        private const val DEFAULT_BODY_WEIGHT_KG = 65.0

        const val MIN_MEALS_PER_DAY = 3
        const val MAX_MEALS_PER_DAY = 6
        val COOK_TIME_OPTIONS = listOf(null, 15, 30, 45)

        private const val KCAL_STEP = 50
        private const val MIN_KCAL = 1200
        private const val MAX_KCAL = 4000
        private const val PROTEIN_STEP_G = 5
        private const val MIN_PROTEIN_G = 40
        private const val MAX_PROTEIN_G = 300
    }
}
