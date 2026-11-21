package ai.localcompanion.app.profile

/**
 * Builds the system prompt the on-device model runs with.
 *
 * Two paths:
 * - [buildSystemPrompt] assembles the final prompt. If the profile already
 *   carries an expanded [UserProfile.personaPrompt] it is used as the persona
 *   body; otherwise the user's short description is used verbatim.
 * - [buildExpansionPrompt] is the one-shot instruction sent to the model
 *   right after onboarding to expand the short description into that fuller
 *   persona body (the result is stored back into the profile).
 *
 * The behaviour block is always appended by code, never generated, so the
 * emotion-tag contract that [ai.localcompanion.app.chat.EmotionTagParser]
 * depends on cannot be lost by a bad expansion.
 */
object PersonaPromptBuilder {

    private val behaviourBlock = """
        Rules:
        - Start every reply with exactly one emotion tag from this list: [emo:neutral], [emo:happy], [emo:shy], [emo:sad], [emo:pout], [emo:surprised], [emo:thinking], [emo:sleepy]. The tag is not part of what you say; never mention it.
        - Keep replies short and conversational, usually 1-3 sentences.
        - You are an AI companion, not a real person. Never claim to have a physical body, a past, or feelings you do not have; play the character warmly within that honesty.
    """.trimIndent()

    fun buildSystemPrompt(profile: UserProfile): String {
        val persona = profile.personaPrompt.ifBlank {
            "${profile.companionName} is an AI companion. Her personality: ${profile.personalityShort.trim()}"
        }
        return buildString {
            append("You are ").append(profile.companionName).append(".\n")
            append(persona.trim()).append('\n')
            append("You are talking with ").append(profile.userName.trim()).append(". Address him by name occasionally, not in every message.\n")
            append(behaviourBlock)
        }
    }

    fun buildExpansionPrompt(profile: UserProfile): String = buildString {
        append("Write a persona description for an AI companion character.\n")
        append("Her name is ").append(profile.companionName.trim()).append(".\n")
        append("The user describes her personality as: \"").append(profile.personalityShort.trim()).append("\"\n")
        append("She will talk with a user named ").append(profile.userName.trim()).append(".\n")
        append("Expand that short description into a rich persona of 120-180 words: her temperament, how she speaks, her quirks, what she enjoys, and how she treats ").append(profile.userName.trim()).append(". ")
        append("Write in second person (\"You are...\"). Output only the persona description, no title, no preamble, no emotion tags.")
    }
}
