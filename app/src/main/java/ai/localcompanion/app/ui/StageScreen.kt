package ai.localcompanion.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ai.localcompanion.app.chat.ChatEngine
import ai.localcompanion.app.chat.EmotionTagParser
import ai.localcompanion.app.models.ModelPacks
import ai.localcompanion.app.models.PackManager
import ai.localcompanion.app.models.PackStatus
import ai.localcompanion.app.profile.PersonaPromptBuilder
import ai.localcompanion.app.profile.ProfileStore
import ai.localcompanion.app.profile.ThemeMode
import ai.localcompanion.app.profile.UserProfile
import ai.localcompanion.app.voice.SttEngine
import ai.localcompanion.app.voice.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ChatLine(val fromUser: Boolean, val text: String)

/**
 * AIRI-style stage: the character owns the full screen; conversation and
 * controls float over her as translucent surfaces. The Live2D renderer
 * mounts into [StageSurface] once the Cubism Core package is dropped in.
 */
@Composable
fun StageScreen(
    profile: UserProfile,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onResetOnboarding: () -> Unit
) {
    val messages = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    var audioMode by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(0.8f) }
    var busy by remember { mutableStateOf(false) }

    val packUi = rememberPackUiState()
    val context = LocalContext.current
    val engine = remember { ChatEngine() }
    val tts = remember { TtsEngine() }
    val stt = remember { SttEngine() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            tts.release()
            stt.release()
        }
    }

    fun voiceDir(): File = PackManager.packDir(context, ModelPacks.VOICE.id)
    fun voiceReady(): Boolean = packUi.status[ModelPacks.VOICE.id] == PackStatus.READY

    /** Speaks a finished reply if the voice pack is present. Best-effort. */
    fun speak(text: String) {
        if (!voiceReady() || text.isBlank()) return
        tts.stop()
        scope.launch {
            try {
                if (!tts.isLoaded) withContext(Dispatchers.IO) { tts.load(voiceDir()) }
                tts.speak(text, volume)
            } catch (e: Exception) {
                // Voice is decoration on top of chat; never surface its errors.
            }
        }
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || busy) return
        messages += ChatLine(true, text)
        val replyIndex = messages.size
        messages += ChatLine(false, "…")
        busy = true
        scope.launch {
            try {
                if (!engine.isLoaded) {
                    val pack = ModelPacks.LFM
                    val modelFile = File(PackManager.packDir(context, pack.id), pack.files.first().fileName)
                    if (!modelFile.exists()) {
                        messages[replyIndex] =
                            ChatLine(false, "Her brain isn't downloaded yet — use the Download button above first.")
                        return@launch
                    }
                    messages[replyIndex] = ChatLine(false, "Waking up…")
                    engine.load(modelFile, PersonaPromptBuilder.buildSystemPrompt(profile))
                    // Best-effort, once: expand her short personality description
                    // into a full persona and persist it for future sessions.
                    if (profile.personaPrompt.isBlank()) {
                        engine.generateOnce(PersonaPromptBuilder.buildExpansionPrompt(profile))
                            ?.let { expanded ->
                                ProfileStore(context).save(profile.copy(personaPrompt = expanded))
                            }
                    }
                    messages[replyIndex] = ChatLine(false, "…")
                }
                val raw = StringBuilder()
                engine.generate(text).collect { chunk ->
                    raw.append(chunk)
                    messages[replyIndex] = ChatLine(false, raw.toString())
                }
                val parsed = EmotionTagParser.parse(raw.toString())
                messages[replyIndex] = ChatLine(false, parsed.spokenText.ifBlank { "…" })
                // parsed.emotion → Live2D expression hook (Cubism slice)
                busy = false
                speak(parsed.spokenText)
            } catch (e: Exception) {
                messages[replyIndex] = ChatLine(false, "⚠ ${e.message ?: "Her brain didn't respond"}")
            } finally {
                busy = false
            }
        }
    }

    fun startListening() {
        scope.launch {
            try {
                if (!stt.isLoaded) withContext(Dispatchers.IO) { stt.load(voiceDir()) }
                listening = true
                val heard = stt.listenOnce()
                listening = false
                if (!heard.isNullOrBlank()) sendMessage(heard)
            } catch (e: Exception) {
                listening = false
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    fun onMicTap() {
        when {
            listening -> {
                stt.stop()
                listening = false
                audioMode = false
                scope.launch(Dispatchers.IO) { stt.release() }
            }
            audioMode -> startListening()
            else -> {
                if (!voiceReady()) {
                    messages += ChatLine(
                        false,
                        "Voice isn't downloaded yet — grab the voice pack in Settings → Models if you want to talk out loud."
                    )
                    return
                }
                audioMode = true
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startListening()
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

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
                packStatus = packUi.status,
                packProgress = packUi.progress,
                onDownloadPack = packUi.download,
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
            val brainStatus = packUi.status[ModelPacks.LFM.id] ?: PackStatus.NOT_DOWNLOADED
            if (brainStatus != PackStatus.READY) {
                BrainPackBanner(
                    status = brainStatus,
                    progress = packUi.progress[ModelPacks.LFM.id] ?: 0f,
                    onDownload = { packUi.download(ModelPacks.LFM) }
                )
                Spacer(Modifier.height(10.dp))
            }
            ChatDock(
                input = input,
                onInputChange = { input = it },
                audioMode = audioMode,
                listening = listening,
                onToggleAudio = { onMicTap() },
                onSend = {
                    if (!busy) {
                        sendMessage(input)
                        input = ""
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Shown above the dock until her brain pack is on the phone. */
@Composable
private fun BrainPackBanner(status: PackStatus, progress: Float, onDownload: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 6.dp
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (status == PackStatus.DOWNLOADING)
                        "Downloading her brain… ${(progress * 100).toInt()}%"
                    else
                        "Her brain (~731 MB) isn't on this phone yet",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (status == PackStatus.NOT_DOWNLOADED) {
                    Button(onClick = onDownload) { Text("Download") }
                }
            }
            if (status == PackStatus.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
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
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        ) {
            Text(
                line.text,
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ChatDock(
    input: String,
    onInputChange: (String) -> Unit,
    audioMode: Boolean,
    listening: Boolean,
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
            // Mic: entering audio mode loads the STT model; leaving unloads it
            // (zero drain in text mode). Red while actually listening.
            Surface(
                shape = CircleShape,
                color = when {
                    listening -> MaterialTheme.colorScheme.error
                    audioMode -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }
            ) {
                IconButton(onClick = onToggleAudio) {
                    Text("🎙", style = MaterialTheme.typography.bodyLarge)
                }
            }
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text(if (listening) "Listening…" else "Message her…") },
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
