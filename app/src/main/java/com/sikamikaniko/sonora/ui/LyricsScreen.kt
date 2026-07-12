package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(vm: SonoraViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.clearAiText(); vm.loadLyrics() }
    val lyrics by vm.lyrics.collectAsState()
    val loading by vm.lyricsLoading.collectAsState()
    val title by vm.title.collectAsState()
    val aiText by vm.aiText.collectAsState()
    val aiStreaming by vm.aiStreaming.collectAsState()
    val enabled by vm.aiEnabled.collectAsState()
    val baseUrl by vm.aiBaseUrl.collectAsState()
    val model by vm.aiModel.collectAsState()
    val aiReady = enabled && baseUrl.isNotBlank() && model.isNotBlank()
    val showAi = aiStreaming || aiText.isNotBlank()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(title ?: "Lyrics", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )

            if (aiReady && !lyrics.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Chip("Translate") { vm.aiLyrics("translate") }
                    Chip("Explain") { vm.aiLyrics("explain") }
                    if (showAi) Chip("Show lyrics") { vm.clearAiText() }
                }
            }

            when {
                showAi -> {
                    if (aiText.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Thinking…", color = MaterialTheme.colorScheme.primary) }
                    } else {
                        Text(
                            aiText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                        )
                    }
                }
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                lyrics.isNullOrBlank() -> CenterMessage("No lyrics found for this track.")
                else -> Text(
                    lyrics ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(label, style = MaterialTheme.typography.bodyMedium) }
}
