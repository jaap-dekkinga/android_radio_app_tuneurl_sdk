package com.tuneurlradio.app.ui.screens.listen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tuneurlradio.app.R
import com.tuneurlradio.app.data.ksal.AdPlacement
import com.tuneurlradio.app.data.ksal.Ksal
import com.tuneurlradio.app.domain.model.Station
import com.tuneurlradio.app.ui.components.ksal.AdSlot
import com.tuneurlradio.app.ui.components.ksal.LinkRow
import com.tuneurlradio.app.ui.components.ksal.SectionHeader
import com.tuneurlradio.app.ui.components.ksal.SmallCapsHeader
import com.tuneurlradio.app.ui.components.ksal.readableWidth
import com.tuneurlradio.app.ui.components.ksal.rememberOpenUrl
import com.tuneurlradio.app.ui.main.MainIntent
import com.tuneurlradio.app.ui.main.MainState

/**
 * Listen tab: the Stream in app / Listen along switch, the stations, a sponsor
 * and on-demand audio. Replaces the Stations tab and the old spyglass button.
 * Port of iOS Flow/Listen/ListenScreen.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit
) {
    val openUrl = rememberOpenUrl()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listen") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            // Two tiles across on phones, three to five on tablets.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.readableWidth().fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ListeningModeSwitcher(state, onIntent)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SmallCapsHeader("Pick a station", Modifier.padding(top = 6.dp))
                }
                items(state.stations, key = { it.id }) { station ->
                    StationTile(
                        station = station,
                        isCurrent = state.currentStation?.id == station.id,
                        isPlaying = state.isPlaying,
                        onClick = { onIntent(MainIntent.SelectStation(station.id)) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AdSlot(AdPlacement.LISTEN, Modifier.padding(vertical = 6.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("On demand", Modifier.padding(top = 4.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
                    ) {
                        listOf(Ksal.pastShows, Ksal.sportsAudio, Ksal.lineup).forEachIndexed { i, link ->
                            if (i > 0) HorizontalDivider(Modifier.padding(start = 52.dp))
                            LinkRow(link) { openUrl(link.url) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Switch
// ---------------------------------------------------------------------------

/** Pill colour for Listen along while the microphone is off. White text on it passes 4.5:1. */
private val PausedGrey = Color(0xFF525252)

private enum class Mode { STREAM, LISTEN_ALONG }

/**
 * The two ways to get Turls, as one switch plus one status row.
 * - Stream in app: the app plays the station and catches Turls in the stream.
 * - Listen along: the app listens through the microphone to a radio nearby.
 * The two never run together: choosing one stops the other.
 */
@Composable
fun ListeningModeSwitcher(state: MainState, onIntent: (MainIntent) -> Unit) {
    val mode = if (state.isPlaying) Mode.STREAM else Mode.LISTEN_ALONG
    val listening = state.isOTAListening
    val accent = MaterialTheme.colorScheme.primary

    fun select(target: Mode) {
        // Listen along is a toggle: tapping it while it is red pauses the
        // microphone, tapping it again starts listening.
        if (target == Mode.LISTEN_ALONG && mode == Mode.LISTEN_ALONG) {
            onIntent(if (listening) MainIntent.StopOTAListening else MainIntent.StartOTAListening)
            return
        }
        if (target == mode) return
        when (target) {
            Mode.STREAM -> onIntent(MainIntent.StartStream)          // also stops the microphone
            Mode.LISTEN_ALONG -> onIntent(MainIntent.StartOTAListening) // also stops the stream
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp)
                .semantics { contentDescription = "Listening mode" }
        ) {
            Segment(
                title = "Stream in app",
                icon = Icons.Filled.PlayArrow,
                active = mode == Mode.STREAM,
                pill = accent,
                hint = null,
                onSelect = { select(Mode.STREAM) }
            )
            // Red with the listening icon while the microphone listens,
            // dark grey with the sleeping speaker while it does not.
            Segment(
                title = "Listen along",
                icon = if (listening) Icons.Filled.GraphicEq else Icons.AutoMirrored.Filled.VolumeOff,
                active = mode == Mode.LISTEN_ALONG,
                pill = if (listening) accent else PausedGrey,
                hint = when {
                    mode != Mode.LISTEN_ALONG -> "Stop the stream and start listening"
                    listening -> "Pause listening"
                    else -> "Start listening"
                },
                onSelect = { select(Mode.LISTEN_ALONG) }
            )
        }

        when (mode) {
            Mode.STREAM -> StreamStatus(state, onIntent)
            Mode.LISTEN_ALONG -> ListenAlongStatus(listening, onIntent)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(
    title: String,
    icon: ImageVector,
    active: Boolean,
    pill: Color,
    hint: String?,
    onSelect: () -> Unit
) {
    val background by animateColorAsState(if (active) pill else Color.Transparent, label = "pill")
    val content = if (active) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
            .then(if (active) Modifier.shadow(3.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(background)
            .clickable(onClickLabel = hint, role = Role.Tab, onClick = onSelect)
            .semantics { selected = active }
            .padding(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = content, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------
// Status rows
// ---------------------------------------------------------------------------

@Composable
private fun StreamStatus(state: MainState, onIntent: (MainIntent) -> Unit) {
    val station = state.currentStation
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = "Open the full player") { onIntent(MainIntent.ExpandPlayer) }
        ) {
            StationLogo(station, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    station?.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Live · catching Turls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        RoundButton(Icons.Filled.Stop, "Stop the stream") { onIntent(MainIntent.StopStream) }
    }
}

@Composable
private fun ListenAlongStatus(listening: Boolean, onIntent: (MainIntent) -> Unit) {
    val tint = if (listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(if (listening) R.drawable.ic_ota_listening else R.drawable.ic_speaker_zzz),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (listening) "Listening for Turls" else "Listening paused", style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep your phone near the radio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // A second pause control on purpose (the user chose to keep both).
        RoundButton(
            if (listening) Icons.Filled.Pause else Icons.Filled.Mic,
            if (listening) "Pause listening" else "Resume listening"
        ) {
            onIntent(if (listening) MainIntent.StopOTAListening else MainIntent.StartOTAListening)
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Icon(icon, contentDescription = label)
    }
}

// ---------------------------------------------------------------------------
// Stations
// ---------------------------------------------------------------------------

@Composable
private fun StationTile(station: Station, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(16.dp))
            .selectable(selected = isCurrent, onClick = onClick)
            .padding(10.dp)
    ) {
        StationLogo(station, Modifier.fillMaxWidth().height(72.dp))
        Column {
            Text(
                station.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isCurrent && isPlaying) "Playing" else station.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A station's logo on white, fitted (logos are wide, the KSAL 1150 art is square). */
@Composable
private fun StationLogo(station: Station?, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (station?.imageURL != null) {
            AsyncImage(
                model = station.imageURL,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            androidx.compose.foundation.Image(
                painterResource(R.drawable.station_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
