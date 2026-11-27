package com.tuneurlradio.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.data.ksal.KsalNewsRepository
import com.tuneurlradio.app.data.ksal.NewsFeed
import com.tuneurlradio.app.data.ksal.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: KsalNewsRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    val weather: StateFlow<WeatherRepository.State> = weatherRepository.state

    private val feeds = mutableMapOf<String, NewsFeed>()

    private val _category = MutableStateFlow(Ksal.topStories)
    val category: StateFlow<Ksal.Category> = _category.asStateFlow()

    private val _feed = MutableStateFlow(feedFor(Ksal.topStories))
    val feed: StateFlow<NewsFeed> = _feed.asStateFlow()

    /** Non-null while search results are shown. */
    private val _searchFeed = MutableStateFlow<NewsFeed?>(null)
    val searchFeed: StateFlow<NewsFeed?> = _searchFeed.asStateFlow()

    init {
        _feed.value.loadIfNeeded()
        viewModelScope.launch { weatherRepository.refreshIfNeeded() }
    }

    private fun feedFor(category: Ksal.Category): NewsFeed =
        feeds.getOrPut(category.id) { NewsFeed(repository, viewModelScope, category) }

    fun selectCategory(category: Ksal.Category) {
        _category.value = category
        val feed = feedFor(category)
        _feed.value = feed
        feed.loadIfNeeded()
    }

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val results = NewsFeed(repository, viewModelScope, Ksal.topStories, trimmed)
        _searchFeed.value = results
        results.reload()
    }

    fun clearSearch() {
        _searchFeed.value = null
    }

    /** Pull to refresh. */
    suspend fun refresh() {
        val search = _searchFeed.value
        if (search != null) {
            search.reload()
            search.awaitLoad()
        } else {
            val feed = _feed.value
            feed.reload()
            viewModelScope.launch { weatherRepository.refresh() }
            feed.awaitLoad()
        }
    }
}
