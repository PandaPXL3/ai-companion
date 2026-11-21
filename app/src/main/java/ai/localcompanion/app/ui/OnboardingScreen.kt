package ai.localcompanion.app.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.localcompanion.app.models.ModelPacks
import ai.localcompanion.app.profile.OnboardingValidator
import ai.localcompanion.app.profile.UserProfile

private enum class Step { WARNING, PERMISSIONS, ABOUT_YOU, COMPANION, DOWNLOAD }

/**
 * First-run setup, AIRI-inspired:
 * 18+ & "AI is not a real person" warning → permissions → your name + age
 * (hard 18+ gate) → her name + short personality (the on-device model later
 * expands it into a full persona prompt) → model packs.
 */
@Composable
fun OnboardingScreen(onComplete: (UserProfile) -> Unit) {
    var step by remember { mutableStateOf(Step.WARNING) }
    var userName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var companionName by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    val packUi = rememberPackUiState()

    val age = ageText.toIntOrNull() ?: 0
    fun draft(companionOk: Boolean) = UserProfile(
        userName = userName,
        userAge = age,
        companionName = if (companionOk) companionName else "companion",
        personalityShort = if (companionOk) personality else "personality"
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (step) {
            Step.WARNING -> WarningStep(onAccept = { step = Step.PERMISSIONS })
            Step.PERMISSIONS -> PermissionsStep(onNext = { step = Step.ABOUT_YOU })
            Step.ABOUT_YOU -> {
                Text("About you", style = MaterialTheme.typography.headlineSmall)
                Text("Your companion uses your name when she talks to you. Your age confirms you're an adult — this app is 18+.")
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Your age") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                ErrorList(errors)
                Button(
                    onClick = {
                        val found = OnboardingValidator.validate(draft(companionOk = false))
                        if (found.isEmpty()) { errors = emptyList(); step = Step.COMPANION } else errors = found
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue") }
            }
            Step.COMPANION -> {
                Text("Create her", style = MaterialTheme.typography.headlineSmall)
                Text("Give her a name and describe her personality in your own words — a sentence or two. Once her brain is downloaded, she'll expand that into her full personality on this device.")
                OutlinedTextField(
                    value = companionName,
                    onValueChange = { companionName = it },
                    label = { Text("Her name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = personality,
                    onValueChange = { if (it.length <= OnboardingValidator.MAX_PERSONALITY_CHARS + 40) personality = it },
                    label = { Text("Her personality, in your words") },
                    placeholder = { Text("e.g. Cheerful and teasing, loves books, a bit shy at first") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${personality.length}/${OnboardingValidator.MAX_PERSONALITY_CHARS}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ErrorList(errors)
                Button(
                    onClick = {
                        val profile = draft(companionOk = true)
                        val found = OnboardingValidator.validate(profile)
                        if (found.isEmpty()) { errors = emptyList(); step = Step.DOWNLOAD } else errors = found
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create her") }
            }
            Step.DOWNLOAD -> {
                Text("Her models", style = MaterialTheme.typography.headlineSmall)
                Text("The app ships empty on purpose — her brain and voice download like game asset packs: resumable and SHA-256 verified. Start the brain now, or later from the stage and Settings → Models.")
                ModelPacks.ALL.forEach { pack ->
                    PackDownloadRow(
                        pack = pack,
                        status = packUi.status[pack.id] ?: ai.localcompanion.app.models.PackStatus.NOT_DOWNLOADED,
                        progress = packUi.progress[pack.id] ?: 0f,
                        onDownload = { packUi.download(pack) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onComplete(draft(companionOk = true)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enter") }
            }
        }
    }
}

@Composable
private fun WarningStep(onAccept: () -> Unit) {
    val context = LocalContext.current
    Text("Before you meet her", style = MaterialTheme.typography.headlineSmall)
    Text(
        "This app is for adults only (18+).",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        "Your companion is an AI — a language model running entirely on this phone. She is not a real person: she has no body, no past, and no feelings, even when she plays a character warmly. " +
            "Please don't treat her as a substitute for real human relationships, and take breaks the way you would with any app."
    )
    Text(
        "Everything — your chats, her memory, your profile — stays on this device. Nothing is uploaded anywhere.",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
        Text("I understand, and I'm 18 or older")
    }
    TextButton(
        onClick = { (context as? Activity)?.finish() },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Exit") }
}

@Composable
private fun PermissionsStep(onNext: () -> Unit) {
    var micGranted by remember { mutableStateOf<Boolean?>(null) }
    var notifGranted by remember { mutableStateOf<Boolean?>(null) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    Text("Permissions", style = MaterialTheme.typography.headlineSmall)
    Text("Only what the features need. Both are optional — you can decide later in Settings too.")
    PermissionRow(
        title = "🎙 Microphone",
        reason = "For audio mode: you talk, she listens (on-device speech recognition). Audio never leaves the phone.",
        granted = micGranted,
        onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    )
    PermissionRow(
        title = "🔔 Notifications",
        reason = "So she can let you know when a model download finishes in the background.",
        granted = notifGranted,
        onRequest = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
}

@Composable
private fun PermissionRow(title: String, reason: String, granted: Boolean?, onRequest: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            when (granted) {
                true -> Text("Allowed ✓", color = MaterialTheme.colorScheme.primary)
                false -> TextButton(onClick = onRequest) { Text("Ask again") }
                null -> TextButton(onClick = onRequest) { Text("Allow") }
            }
        }
        Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorList(errors: List<String>) {
    errors.forEach { error ->
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
