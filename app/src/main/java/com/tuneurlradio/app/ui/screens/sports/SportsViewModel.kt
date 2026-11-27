package com.tuneurlradio.app.ui.screens.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.data.ksal.KsalNewsRepository
import com.tuneurlradio.app.data.ksal.NewsFeed
import com.tuneurlradio.app.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SportsViewModel @Inject constructor(
    repository: KsalNewsRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    val feed = NewsFeed(repository, viewModelScope, Ksal.sportsNews)

    /** Followed teams, in KSAL order. Saved as `followed_teams`. */
    val followedTeams: StateFlow<List<Ksal.Team>> = settings.followedTeams
        .map { ids -> Ksal.teams.filter { it.id in ids } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ksal.teams)

    init {
        feed.loadIfNeeded()
    }

    fun setFollowing(team: Ksal.Team, follow: Boolean) {
        val ids = followedTeams.value.map { it.id }.toMutableSet()
        if (follow) ids.add(team.id) else ids.remove(team.id)
        viewModelScope.launch { settings.setFollowedTeams(Ksal.teams.map { it.id }.filter { it in ids }) }
    }

    suspend fun refresh() {
        feed.reload()
        feed.awaitLoad()
    }
}
