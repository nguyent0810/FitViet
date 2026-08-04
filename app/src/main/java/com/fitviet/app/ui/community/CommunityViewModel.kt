package com.fitviet.app.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitviet.app.data.local.entity.CommunityPostEntity
import com.fitviet.app.data.repository.CommunityRepository
import com.fitviet.app.domain.CommunityFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CommunityUiState(
    val posts: List<CommunityPostEntity> = emptyList(),
    val selectedTab: Int = 0,
)

class CommunityViewModel(private val repository: CommunityRepository) : ViewModel() {
    private val selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<CommunityUiState> = combine(repository.observe(), selectedTab) { posts, tab ->
        CommunityUiState(posts = CommunityFilter.byTab(posts, tab), selectedTab = tab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunityUiState())

    fun selectTab(tab: Int) {
        selectedTab.value = tab
    }

    fun toggleLike(post: CommunityPostEntity) {
        viewModelScope.launch { repository.toggleLike(post) }
    }

    class Factory(private val repository: CommunityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CommunityViewModel(repository) as T
    }
}
