package com.fitviet.app.ui.nutrition.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.repository.RecipeFilter
import com.fitviet.app.data.repository.RecipeRepository
import com.fitviet.app.domain.RecipeWithNutrition
import com.fitviet.app.util.normalizeVietnamese
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One of the 5 fixed filter chips from the design brief — [tag] matches
 * [com.fitviet.app.data.local.entity.RecipeEntity.tags]' literal seed values exactly (e.g.
 * "Giảm mỡ", "≤15 phút"), same "raw Vietnamese tag string, no separate i18n layer" idiom the
 * existing Programs filter chips already use. */
data class DiscoverFilterChip(val labelRes: Int, val tag: String)

val DISCOVER_FILTER_CHIPS = listOf(
    DiscoverFilterChip(com.fitviet.app.R.string.nutrition_filter_low_cal, "Giảm mỡ"),
    DiscoverFilterChip(com.fitviet.app.R.string.nutrition_filter_high_protein, "Giàu protein"),
    DiscoverFilterChip(com.fitviet.app.R.string.nutrition_filter_quick, "≤15 phút"),
    DiscoverFilterChip(com.fitviet.app.R.string.nutrition_filter_vietnamese, "Món Việt"),
    DiscoverFilterChip(com.fitviet.app.R.string.nutrition_filter_easy, "Dễ làm"),
)

data class DiscoverUiState(
    val searchQuery: String = "",
    /** Multi-select — a recipe must match ALL selected tags (see [RecipeFilter.tags]'s
     * `any { it in recipe.tags }` semantics... actually AND across chips, OR within
     * [RecipeFilter.tags] itself — this ViewModel narrows one filter call at a time per selected
     * chip via successive [RecipeFilter] application, see [uiState]'s doc). */
    val selectedTags: Set<String> = emptySet(),
    val recipes: List<RecipeWithNutrition> = emptyList(),
)

class DiscoverViewModel(private val recipeRepository: RecipeRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedTags = MutableStateFlow<Set<String>>(emptySet())

    /** [RecipeFilter.tags] itself only expresses "matches ANY of these tags" (an OR, per
     * [RecipeRepository.observeRecipes]'s contract) — multi-selecting several chips here should
     * narrow (AND) rather than broaden, so each selected tag is applied as its own successive
     * client-side intersection rather than passed through as one [RecipeFilter.tags] set. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DiscoverUiState> = combine(searchQuery, selectedTags) { query, tags -> query to tags }
        .flatMapLatest { (query, tags) ->
            val normalizedQuery = query.takeIf { it.isNotBlank() }?.let(::normalizeVietnamese)
            recipeRepository.observeRecipes(RecipeFilter(searchQuery = normalizedQuery)).map { recipes ->
                val filtered = if (tags.isEmpty()) recipes else recipes.filter { recipe -> tags.all { it in recipe.tags } }
                DiscoverUiState(searchQuery = query, selectedTags = tags, recipes = filtered)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun toggleTag(tag: String) {
        selectedTags.value = if (tag in selectedTags.value) selectedTags.value - tag else selectedTags.value + tag
    }

    class Factory(private val recipeRepository: RecipeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DiscoverViewModel(recipeRepository) as T
    }
}
