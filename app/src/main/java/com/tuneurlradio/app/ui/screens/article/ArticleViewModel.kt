package com.tuneurlradio.app.ui.screens.article

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.data.ksal.KsalNewsRepository
import com.tuneurlradio.app.data.ksal.NewsArticle
import com.tuneurlradio.app.data.local.SettingsDataStore
import com.tuneurlradio.app.data.local.StationsDataSource
import com.tuneurlradio.app.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: KsalNewsRepository,
    stationsDataSource: StationsDataSource,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val articleId: Int = savedStateHandle.get<Int>(ARG_ID) ?: -1

    /** From the list cache. Null after process death: the screen then shows a fallback. */
    val article: NewsArticle? = repository.cached(articleId)

    val newsStation: Station? = stationsDataSource.loadStations().let { stations ->
        stations.firstOrNull { it.id == Ksal.NEWS_STATION_ID } ?: stations.firstOrNull()
    }

    val textScale: StateFlow<Float> =
        settings.articleTextScale.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)

    fun cycleTextSize() {
        val index = SCALES.indexOf(textScale.value).let { if (it < 0) 1 else it }
        viewModelScope.launch { settings.setArticleTextScale(SCALES[(index + 1) % SCALES.size]) }
    }

    companion object {
        const val ARG_ID = com.tuneurlradio.app.navigation.NavArgs.ARTICLE_ID
        private val SCALES = listOf(0.9f, 1.0f, 1.15f, 1.3f)
    }
}
