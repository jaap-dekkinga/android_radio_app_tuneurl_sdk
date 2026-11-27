package com.tuneurlradio.app.ui.screens.local

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuneurlradio.app.data.ksal.AdPlacement
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.data.ksal.WeatherRepository
import com.tuneurlradio.app.ui.components.ksal.AdSlot
import com.tuneurlradio.app.ui.components.ksal.SmallCapsHeader
import com.tuneurlradio.app.ui.components.ksal.readableWidth
import com.tuneurlradio.app.ui.components.ksal.rememberOpenUrl
import kotlinx.coroutines.launch

/** Local tab: Salina weather, closings and the ksal.com community pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalScreen(viewModel: LocalViewModel = hiltViewModel()) {
    val weather by viewModel.weather.collectAsState()
    val openUrl = rememberOpenUrl()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; viewModel.refresh(); refreshing = false } },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Four tiles across on phones, more on tablets.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.readableWidth().fillMaxSize().align(Alignment.TopCenter)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    WeatherCard(weather, openUrl)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AdSlot(AdPlacement.LOCAL, Modifier.padding(vertical = 8.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SmallCapsHeader("Community", Modifier.padding(top = 8.dp))
                }
                items(Ksal.communityLinks, key = { it.id }) { link ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .heightIn(min = 78.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { openUrl(link.url) }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(link.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(6.dp))
                        Text(
                            link.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherRepository.State, openUrl: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Salina weather",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val now = weather.conditions
                when {
                    now != null -> {
                        Text("${now.temperature}°${now.unit}", fontSize = 40.sp, fontWeight = FontWeight.Light)
                        Text(now.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                    weather.isLoading -> CircularProgressIndicator(Modifier.padding(vertical = 8.dp).size(28.dp))
                    else -> Text(
                        "Weather unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Outlined.WbCloudy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Ksal.forecast, Ksal.radar, Ksal.roads).forEach { link ->
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { openUrl(link.url) }
                        .padding(vertical = 12.dp)
                )
            }
        }

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clickable { openUrl(Ksal.closings.url) }
        ) {
            Icon(Ksal.closings.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Local closings & delays", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "Weather data: National Weather Service",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
