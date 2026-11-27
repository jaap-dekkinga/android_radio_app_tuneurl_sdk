package com.tuneurlradio.app.ui.screens.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuneurlradio.app.data.ksal.AdPlacement
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.ui.components.ksal.AdSlot
import com.tuneurlradio.app.ui.components.ksal.HeroArticleCard
import com.tuneurlradio.app.ui.components.ksal.LinkRow
import com.tuneurlradio.app.ui.components.ksal.SmallCapsHeader
import com.tuneurlradio.app.ui.components.ksal.isTablet
import com.tuneurlradio.app.ui.components.ksal.readableWidth
import com.tuneurlradio.app.ui.components.ksal.rememberOpenUrl
import com.tuneurlradio.app.ui.screens.home.articleRows
import com.tuneurlradio.app.ui.screens.home.feedEnd
import kotlinx.coroutines.launch

private enum class SportsSection(val title: String) { NEWS("News"), SCORES("Scores"), AUDIO("Audio") }

/** Sports tab: followed teams, then News / Scores / Audio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsScreen(
    onOpenArticle: (Int) -> Unit,
    viewModel: SportsViewModel = hiltViewModel()
) {
    var section by rememberSaveable { mutableStateOf(SportsSection.NEWS) }
    var editingTeams by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val followed by viewModel.followedTeams.collectAsState()
    val feedState by viewModel.feed.state.collectAsState()
    val openUrl = rememberOpenUrl()
    val scope = rememberCoroutineScope()
    val columns = if (isTablet()) 2 else 1
    val padded = Modifier.readableWidth().padding(horizontal = 16.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sports") },
                actions = {
                    TextButton(onClick = { editingTeams = true }) { Text("Edit") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; viewModel.refresh(); refreshing = false } },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "picker") {
                    SingleChoiceSegmentedButtonRow(padded) {
                        SportsSection.entries.forEachIndexed { i, s ->
                            SegmentedButton(
                                selected = section == s,
                                onClick = { section = s },
                                shape = SegmentedButtonDefaults.itemShape(i, SportsSection.entries.size)
                            ) { Text(s.title) }
                        }
                    }
                }
                item(key = "teams") {
                    Column(padded, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallCapsHeader("My teams")
                        if (followed.isEmpty()) {
                            TextButton(onClick = { editingTeams = true }) { Text("Choose teams to follow") }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                followed.forEach { team ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(onClickLabel = team.name) { openUrl(team.url) }
                                    ) {
                                        Box(
                                            Modifier.size(56.dp).clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(team.short, fontWeight = FontWeight.Black)
                                        }
                                        Text(team.name, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "ad") { AdSlot(AdPlacement.SPORTS, padded) }

                when (section) {
                    SportsSection.NEWS -> {
                        item(key = "latest") { SmallCapsHeader("Latest", padded) }
                        feedState.articles.firstOrNull()?.let { lead ->
                            item(key = "lead_${lead.id}") {
                                HeroArticleCard(lead, onClick = { onOpenArticle(lead.id) }, modifier = padded)
                            }
                        }
                        articleRows("sp", feedState.articles.drop(1), columns, onOpenArticle)
                        feedEnd(viewModel.feed, feedState)
                    }
                    SportsSection.SCORES -> item(key = "scores") {
                        LinkCard(padded) {
                            LinkRow(Ksal.scoreboard, "Area scores, schedules and broadcast results") { openUrl(Ksal.scoreboard.url) }
                        }
                    }
                    SportsSection.AUDIO -> item(key = "audio") {
                        LinkCard(padded) {
                            LinkRow(Ksal.sportsAudio, "Interviews and game audio") { openUrl(Ksal.sportsAudio.url) }
                            HorizontalDivider()
                            LinkRow(Ksal.pastShows, "Past shows on SoundCloud") { openUrl(Ksal.pastShows.url) }
                        }
                    }
                }
            }
        }
    }

    if (editingTeams) {
        ModalBottomSheet(onDismissRequest = { editingTeams = false }) {
            Text(
                "My Teams",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Ksal.teams.forEach { team ->
                val on = followed.any { it.id == team.id }
                ListItem(
                    headlineContent = { Text(team.name) },
                    trailingContent = {
                        Switch(checked = on, onCheckedChange = { viewModel.setFollowing(team, it) })
                    }
                )
            }
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { editingTeams = false }) { Text("Done") }
            }
        }
    }
}

@Composable
internal fun LinkCard(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}
