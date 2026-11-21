package ai.localcompanion.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ai.localcompanion.app.models.ModelPack
import ai.localcompanion.app.models.ModelPacks
import ai.localcompanion.app.models.PackManager
import ai.localcompanion.app.models.PackStatus

/** Live download state for every pack, backed by WorkManager + disk. */
data class PackUiState(
    val status: Map<String, PackStatus>,
    val progress: Map<String, Float>,
    val download: (ModelPack) -> Unit
)

@Composable
fun rememberPackUiState(): PackUiState {
    val context = LocalContext.current
    val lfm = rememberSinglePackState(ModelPacks.LFM)
    val voice = rememberSinglePackState(ModelPacks.VOICE)
    return PackUiState(
        status = mapOf(ModelPacks.LFM.id to lfm.first, ModelPacks.VOICE.id to voice.first),
        progress = mapOf(ModelPacks.LFM.id to lfm.second, ModelPacks.VOICE.id to voice.second),
        download = { pack -> PackManager.enqueue(context, pack) }
    )
}

@Composable
private fun rememberSinglePackState(pack: ModelPack): Pair<PackStatus, Float> {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val infos by workManager.getWorkInfosForUniqueWorkFlow(PackManager.workName(pack.id))
        .collectAsState(initial = emptyList())
    val info = infos.firstOrNull()
    val status = when {
        PackManager.isReady(context, pack) -> PackStatus.READY
        info?.state == WorkInfo.State.RUNNING || info?.state == WorkInfo.State.ENQUEUED ->
            PackStatus.DOWNLOADING
        else -> PackStatus.NOT_DOWNLOADED
    }
    val progress = info?.progress?.getFloat(PackManager.KEY_PROGRESS, 0f) ?: 0f
    return status to progress
}

/** One pack row: title, size, and a Download button / progress / Ready mark. */
@Composable
fun PackDownloadRow(
    pack: ModelPack,
    status: PackStatus,
    progress: Float,
    onDownload: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pack.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${pack.description} · ~${pack.approxSizeMb} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (status) {
                PackStatus.READY -> Text("Ready ✓", color = MaterialTheme.colorScheme.primary)
                PackStatus.DOWNLOADING -> Text("${(progress * 100).toInt()}%")
                PackStatus.NOT_DOWNLOADED -> OutlinedButton(onClick = onDownload) { Text("Download") }
            }
        }
        if (status == PackStatus.DOWNLOADING) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}
