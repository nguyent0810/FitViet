package com.fitviet.app.ui.nutrition.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.dao.SettingsDao
import com.fitviet.app.data.local.entity.FoodEntity
import com.fitviet.app.data.local.entity.MealPlanTemplateEntity
import com.fitviet.app.data.repository.MealPlanRepository
import com.fitviet.app.data.repository.NutritionRepository
import com.fitviet.app.data.repository.RecipeFilter
import com.fitviet.app.data.repository.RecipeRepository
import com.fitviet.app.domain.NutritionGoal
import com.fitviet.app.domain.NutritionGoals
import com.fitviet.app.domain.RecipeWithNutrition
import com.fitviet.app.util.normalizeVietnamese
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Redesign Gate 5b-i — the two tabs plan-checked with the reviewer as folding cleanly into this
 * gate; "Kế hoạch tuần" (Gate 5b-ii) isn't rendered as a pill yet at all, per that plan-check's own
 * "present-but-dead is worse than absent for one gate" call. */
enum class LibraryTab { RECIPES, TEMPLATES }

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
)

/** Merges what used to be `DiscoverViewModel`/`FoodsViewModel`/`TemplatesViewModel` into one VM
 * with tab-scoped state — the plan-checked precedent for this merge is [com.fitviet.app.ui.handbook.HandbookViewModel]
 * (one VM, `selectedTab` in state, two unrelated data sources) and [com.fitviet.app.ui.programs.ProgramsViewModel]'s
 * own multi-source staging (`ProgramsStage1`), not three surviving child VMs — the reviewer's own
 * ruling was that Gate 3c's `QuickGenerateViewModel` (which DID stay separate) doesn't apply here
 * since that sheet has multiple unrelated host screens where this screen's three tabs have exactly
 * one host. Past 5 source flows there's no typed `combine{}` overload, so this follows
 * [com.fitviet.app.ui.programs.ProgramsViewModel]'s own staged-combine idiom rather than nesting. */
class NutritionLibraryViewModel(
    private val recipeRepository: RecipeRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val settingsDao: SettingsDao,
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedTab = MutableStateFlow(LibraryTab.RECIPES)
    private val isApplyingTemplate = MutableStateFlow(false)

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

    val uiState: StateFlow<LibraryUiState> = stage1
        .combine(nutritionRepository.observe()) { stage, nutritionData -> stage to nutritionData.totals.kcal }
        .combine(isApplyingTemplate) { (stage, kcalToday), applying ->
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
                isApplyingTemplate = applying,
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

    /** Same re-entrant-tap guard as `TemplatesViewModel.useTemplate` (the Quick Generate CTA bug
     * precedent) — kept as an identical copy rather than a shared helper since the two VMs have no
     * other relationship and `TemplatesViewModel` itself is retired by this same gate. */
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

    class Factory(
        private val recipeRepository: RecipeRepository,
        private val mealPlanRepository: MealPlanRepository,
        private val settingsDao: SettingsDao,
        private val nutritionRepository: NutritionRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NutritionLibraryViewModel(recipeRepository, mealPlanRepository, settingsDao, nutritionRepository) as T
    }
}
