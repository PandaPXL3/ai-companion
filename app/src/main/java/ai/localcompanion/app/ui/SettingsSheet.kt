package ai.localcompanion.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.localcompanion.app.models.ModelPack
import ai.localcompanion.app.models.ModelPacks
import ai.localcompanion.app.models.PackStatus
import ai.localcompanion.app.profile.ThemeMode
import ai.localcompanion.app.profile.UserProfile

/**
 * AIRI-style settings: one bottom sheet, grouped sections —
 * Appearance / Sound / Models / Memory / Data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    profile: UserProfile,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    packStatus: Map<String, PackStatus>,
    packProgress: Map<String, Float>,
    onDownloadPack: (ModelPack) -> Unit,
    onResetOnboarding: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "${profile.companionName} · talking with ${profile.userName}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))

            SectionHeader("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionHeader("Sound")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(14.dp))

            SectionHeader("Models")
            ModelPacks.ALL.forEach { pack ->
                PackDownloadRow(
                    pack = pack,
                    status = packStatus[pack.id] ?: PackStatus.NOT_DOWNLOADED,
                    progress = packProgress[pack.id] ?: 0f,
                    onDownload = { onDownloadPack(pack) }
                )
            }
            Spacer(Modifier.height(14.dp))

            SectionHeader("Memory")
            Text(
                "Conversation memory lives on this device and is summarized automatically. Nothing leaves the phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            SectionHeader("Data")
            TextButton(onClick = onResetOnboarding) {
                Text("Reset profile & onboarding", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

