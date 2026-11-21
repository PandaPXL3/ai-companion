package ai.localcompanion.app.profile

/**
 * Everything the first-run onboarding collects. [personaPrompt] is filled in
 * after the on-device model expands [personalityShort] into a full persona;
 * until then it stays empty and [PersonaPromptBuilder.buildSystemPrompt] is
 * used directly.
 */
data class UserProfile(
    val userName: String,
    val userAge: Int,
    val companionName: String,
    val personalityShort: String,
    val personaPrompt: String = ""
)
