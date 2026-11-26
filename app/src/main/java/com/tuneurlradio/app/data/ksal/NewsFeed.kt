package com.tuneurlradio.app.data.ksal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A paged list of ksal.com stories for one category or one search.
 * Port of iOS NewsFeed.swift. Lives in a ViewModel's scope.
 */
class NewsFeed(
    private val repository: KsalNewsRepository,
    private val scope: CoroutineScope,
    val category: Ksal.Category,
    val searchQuery: String? = null
) {
    data class State(
        val articles: List<NewsArticle> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val canLoadMore: Boolean = true
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var nextPage = 1
    private var totalPages: Int? = null
    private var job: Job? = null

    /** Loads page 1 again. Keeps the old list visible until the new one arrives. */
    fun reload() {
        job?.cancel()
        nextPage = 1
        totalPages = null
        _state.value = _state.value.copy(canLoadMore = true)
        loadPage(replacing = true)
    }

    /** Loads the first page only if nothing is loaded yet. */
    fun loadIfNeeded() {
        val s = _state.value
        if (s.articles.isEmpty() && !s.isLoading) reload()
    }

    /** Call when the last row appears. */
    fun loadMore() {
        val s = _state.value
        if (!s.canLoadMore || s.isLoading || s.errorMessage != null) return
        loadPage(replacing = false)
    }

    /** After an error: try the failed page again. */
    fun retry() {
        if (_state.value.articles.isEmpty()) reload() else loadPage(replacing = false)
    }

    /** Suspends until the current load finishes (for pull to refresh). */
    suspend fun awaitLoad() {
        job?.join()
    }

    private fun loadPage(replacing: Boolean) {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        job = scope.launch {
            repository.fetchPage(category, nextPage, searchQuery)
                .onSuccess { page ->
                    page.totalPages?.let { totalPages = it }
                    if (page.pastEnd) {
                        _state.value = _state.value.copy(isLoading = false, canLoadMore = false)
                        return@onSuccess
                    }
                    val current = _state.value.articles
                    val merged = if (replacing) page.articles else {
                        val known = current.map { it.id }.toHashSet()
                        current + page.articles.filter { it.id !in known }
                    }
                    nextPage += 1
                    val more = page.articles.isNotEmpty() && (totalPages?.let { nextPage <= it } ?: true)
                    _state.value = State(articles = merged, isLoading = false, canLoadMore = more)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not load stories"
                    )
                }
        }
    }
}
