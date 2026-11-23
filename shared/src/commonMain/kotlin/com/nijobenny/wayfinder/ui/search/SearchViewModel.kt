package com.nijobenny.wayfinder.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.repository.GeocodingRepository
import com.nijobenny.wayfinder.domain.repository.PlacesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

sealed interface SearchResults {
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Ready(val title: String?, val places: List<Place>) : SearchResults
    data object Empty : SearchResults
    data object Error : SearchResults
}

data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults.Idle,
    val activeCategory: Category? = null,
)

/** Autocomplete search with debouncing, category browsing and recent searches. */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val geocoding: GeocodingRepository,
    private val places: PlacesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recents: StateFlow<List<Place>> = places.recentSearches.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val saved: StateFlow<List<SavedPlace>> = places.savedPlaces.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val queries = MutableStateFlow("")
    private var near: LatLng? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            queries
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { q -> runSearch(q) }
        }
    }

    fun setNear(location: LatLng?) {
        near = location
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, activeCategory = null) }
        if (query.isBlank()) _state.update { it.copy(results = SearchResults.Idle) }
        queries.value = query
    }

    fun onCategory(category: Category) {
        searchJob?.cancel()
        _state.update { it.copy(query = "", activeCategory = category, results = SearchResults.Loading) }
        queries.value = ""
        searchJob = viewModelScope.launch {
            val result = geocoding.nearby(category, near)
            _state.update { s ->
                if (s.activeCategory != category) s
                else s.copy(results = result.toResults("${category.emoji} ${category.label} nearby"))
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        queries.value = ""
        _state.value = SearchUiState()
    }

    fun clearRecents() {
        viewModelScope.launch { places.clearRecents() }
    }

    private fun runSearch(query: String) {
        if (query.trim().length < MIN_QUERY) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(results = SearchResults.Loading) }
            val result = geocoding.search(query.trim(), near)
            _state.update { s -> if (s.query != query) s else s.copy(results = result.toResults(null)) }
        }
    }

    private fun Result<List<Place>>.toResults(title: String?): SearchResults = fold(
        onSuccess = { if (it.isEmpty()) SearchResults.Empty else SearchResults.Ready(title, it) },
        onFailure = { SearchResults.Error },
    )

    companion object {
        const val DEBOUNCE_MS = 350L
        const val MIN_QUERY = 2
    }
}
