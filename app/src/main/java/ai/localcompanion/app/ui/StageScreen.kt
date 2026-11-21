package ai.localcompanion.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class ChatLine(val fromUser: Boolean, val text: String)

/**
 * AIRI-style stage: the character owns the full screen; conversation and
 * controls float over her as translucent surfaces. The Live2D renderer
 * mounts into [StageSurface] once the Cubism Core package is dropped in.
 */
@Composable
fun StageScreen(
    profile: ai.localcompanion.app.profile.UserProfile,
    themeMode: ai.localcompanion.app.profile.ThemeMode,
    onThemeModeChange: (ai.localcompanion.app.profile.ThemeMode) -> Unit,
    onResetOnboarding: () -> Unit
) {
    val messages = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    var audioMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(0.8f) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StageSurface(Modifier.fillMaxSize())

        // Top bar: her name on the left, one clearly visible menu button on
        // the right (the old faint overlay icons were nearly invisible).
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Text(
                    profile.companionName,
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                IconButton(onClick = { showSettings = true }, Modifier.size(52.dp)) {
                    Text("☰", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        if (showSettings) {
            SettingsSheet(
                profile = profile,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                volume = volume,
                onVolumeChange = { volume = it },
                packStatus = emptyMap(),
                packProgress = emptyMap(),
                onDownloadPack = { /* TODO: wired by the downloader slice */ },
                onResetOnboarding = { showSettings = false; onResetOnboarding() },
                onDismiss = { showSettings = false }
            )
        }

        // Conversation overlay: lower third only, never covering her face
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages.takeLast(4)) { line ->
                    ChatBubble(line)
                }
            }
            Spacer(Modifier.height(10.dp))
            ChatDock(
                input = input,
                onInputChange = { input = it },
                audioMode = audioMode,
                onToggleAudio = { audioMode = !audioMode },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        messages += ChatLine(true, text)
                        input = ""
                        // TODO: route through ChatEngine (LEAP LFM) once models are downloaded
                        messages += ChatLine(false, "…")
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Placeholder stage. The Live2D Cubism view replaces this composable. */
@Composable
private fun StageSurface(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            "Live2D stage\n(import a model to begin)",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        ) {
            Text(line.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChatDock(
    input: String,
    onInputChange: (String) -> Unit,
    audioMode: Boolean,
    onToggleAudio: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 6.dp
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Mic toggle: ON loads the STT model; OFF unloads it (zero drain)
            Surface(
                shape = CircleShape,
                color = if (audioMode) MaterialTheme.colorScheme.primary else Color.Transparent
            ) {
                IconButton(onClick = onToggleAudio) {
                    Text("🎙", style = MaterialTheme.typography.bodyLarge)
                }
            }
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Message her…") },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 3
            )
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                IconButton(onClick = onSend) {
                    Text("➤", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
