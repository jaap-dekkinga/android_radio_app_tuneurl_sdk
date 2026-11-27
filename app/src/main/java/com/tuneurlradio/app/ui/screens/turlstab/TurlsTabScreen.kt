package com.tuneurlradio.app.ui.screens.turlstab

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuneurlradio.app.ui.components.ksal.SettingsGear
import com.tuneurlradio.app.ui.components.ksal.readableWidth
import com.tuneurlradio.app.ui.screens.saved.SavedEngagementsScreen
import com.tuneurlradio.app.ui.screens.turls.TurlsHistoryScreen

private enum class TurlsMode(val title: String) { SAVED("Saved"), HEARD("Heard") }

/** Turls tab: saved Turls and the history of heard Turls, behind one switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurlsTabScreen(
    onOpenSaved: (Long) -> Unit,
    onOpenHeard: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(TurlsMode.SAVED) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Turls") },
                actions = { SettingsGear(onOpenSettings) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.readableWidth()) {
                    TurlsMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = mode == m,
                            onClick = { mode = m },
                            shape = SegmentedButtonDefaults.itemShape(i, TurlsMode.entries.size)
                        ) { Text(m.title) }
                    }
                }
            }
            when (mode) {
                TurlsMode.SAVED -> SavedEngagementsScreen(onItemOpen = onOpenSaved, embedded = true)
                TurlsMode.HEARD -> TurlsHistoryScreen(onItemOpen = onOpenHeard, embedded = true)
            }
        }
    }
}
