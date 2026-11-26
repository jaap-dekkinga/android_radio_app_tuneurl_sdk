package com.tuneurlradio.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuneurlradio.app.data.ksal.AdPlacement
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.data.ksal.NewsArticle
import com.tuneurlradio.app.data.ksal.NewsFeed
import com.tuneurlradio.app.ui.components.ksal.AdSlot
import com.tuneurlradio.app.ui.components.ksal.ArticleList
import com.tuneurlradio.app.ui.components.ksal.CategoryChips
import com.tuneurlradio.app.ui.components.ksal.FeedFooter
import com.tuneurlradio.app.ui.components.ksal.HeroArticleCard
import com.tuneurlradio.app.ui.components.ksal.SectionHeader
import com.tuneurlradio.app.ui.components.ksal.SettingsGear
import com.tuneurlradio.app.ui.components.ksal.WeatherStrip
import com.tuneurlradio.app.ui.components.ksal.isTablet
import com.tuneurlradio.app.ui.components.ksal.Wordmark
import com.tuneurlradio.app.ui.components.ksal.readableWidth
import kotlinx.coroutines.launch

/** Home tab: KSAL header, weather strip, category chips, lead story and the latest list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenArticle: (Int) -> Unit,
    onOpenLocal: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val weather by viewModel.weather.collectAsState()
    val category by viewModel.category.collectAsState()
    val feed by viewModel.feed.collectAsState()
    val searchFeed by viewModel.searchFeed.collectAsState()

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val feedState by remember(feed) { feed.state }.collectAsState()
    val searchState by remember(searchFeed) {
        searchFeed?.state ?: kotlinx.coroutines.flow.MutableStateFlow(NewsFeed.State())
    }.collectAsState()
    val columns = if (isTablet()) 2 else 1

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Wordmark() },
                actions = {
                    IconButton(onClick = {
                        if (searchOpen) {
                            searchText = ""
                            viewModel.clearSearch()
                        }
                        searchOpen = !searchOpen
                    }) {
                        Icon(
                            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchOpen) "Close search" else "Search KSAL news"
                        )
                    }
                    SettingsGear(onOpenSettings)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    viewModel.refresh()
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (searchOpen) {
                    item(key = "search") {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                if (it.isEmpty()) viewModel.clearSearch()
                            },
                            placeholder = { Text("Search KSAL news") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchText) }),
                            modifier = Modifier.readableWidth().padding(horizontal = 16.dp)
                        )
                    }
                }

                val search = searchFeed
                if (search != null) {
                    searchResults(search, searchState, columns, onOpenArticle)
                } else {
                    item(key = "weather") {
                        WeatherStrip(
                            weather,
                            onClick = onOpenLocal,
                            modifier = Modifier.readableWidth().padding(horizontal = 16.dp)
                        )
                    }
                    // Top banner, the same zone as the top of ksal.com.
                    item(key = "ad_top") {
                        AdSlot(AdPlacement.HOME_TOP, Modifier.readableWidth().padding(horizontal = 16.dp))
                    }
                    item(key = "chips") {
                        Box(Modifier.readableWidth()) {
                            CategoryChips(Ksal.homeCategories, category, viewModel::selectCategory)
                        }
                    }
                    feedContent(feed, feedState, columns, onOpenArticle)
                }
            }
        }
    }
}

internal fun LazyListScope.articleRows(
    keyPrefix: String,
    articles: List<NewsArticle>,
    columns: Int,
    onOpenArticle: (Int) -> Unit
) {
    val rows = articles.chunked(columns)
    items(rows.size, key = { "${keyPrefix}_${rows[it].first().id}" }) { index ->
        ArticleList(
            rows[index],
            onOpen = { onOpenArticle(it.id) },
            modifier = Modifier.readableWidth().padding(horizontal = 16.dp)
        )
    }
}

private fun LazyListScope.feedContent(
    feed: NewsFeed,
    state: NewsFeed.State,
    columns: Int,
    onOpenArticle: (Int) -> Unit
) {
    val articles = state.articles
    val padded = Modifier.readableWidth().padding(horizontal = 16.dp)
    articles.firstOrNull()?.let { lead ->
        item(key = "lead_${feed.category.id}_${lead.id}") {
            HeroArticleCard(lead, onClick = { onOpenArticle(lead.id) }, modifier = padded)
        }
    }
    if (articles.size > 1) {
        val rest = articles.drop(1)
        item(key = "latest_${feed.category.id}") {
            SectionHeader("Latest", padded.padding(top = 4.dp))
        }
        articleRows("a_${feed.category.id}", rest.take(5), columns, onOpenArticle)
        // In-feed sponsor after the first five stories.
        item(key = "ad_feed") { AdSlot(AdPlacement.HOME_FEED, padded) }
        articleRows("b_${feed.category.id}", rest.drop(5), columns, onOpenArticle)
    }
    feedEnd(feed, state)
}

private fun LazyListScope.searchResults(
    feed: NewsFeed,
    state: NewsFeed.State,
    columns: Int,
    onOpenArticle: (Int) -> Unit
) {
    if (state.articles.isEmpty() && !state.isLoading && state.errorMessage == null) {
        item(key = "no_results") {
            Text(
                "No results for \u201c${feed.searchQuery}\u201d",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.readableWidth().padding(horizontal = 16.dp, vertical = 32.dp)
            )
        }
    } else {
        articleRows("s", state.articles, columns, onOpenArticle)
    }
    feedEnd(feed, state)
}

/**
 * The end of the list: loads the next page when it scrolls into view, then shows
 * the spinner or the error. A lazy item is only composed when it is (nearly) on
 * screen, so this does not chain-load every page.
 */
internal fun LazyListScope.feedEnd(feed: NewsFeed, state: NewsFeed.State) {
    item(key = "end_${feed.category.id}_${feed.searchQuery}") {
        LaunchedEffect(feed, state.articles.size) {
            if (state.articles.isNotEmpty()) feed.loadMore()
        }
        FeedFooter(state.isLoading, state.errorMessage, onRetry = feed::retry)
    }
}
