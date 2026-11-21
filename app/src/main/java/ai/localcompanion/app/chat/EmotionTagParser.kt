package ai.localcompanion.app.chat

enum class Emotion {
    NEUTRAL, HAPPY, SHY, SAD, POUT, SURPRISED, THINKING, SLEEPY
}

data class ParsedReply(val emotion: Emotion, val spokenText: String)

/**
 * Replies from the LLM must start with an emotion tag: `[emo:shy] text...`.
 * The tag drives the Live2D expression/motion and is stripped before TTS.
 */
object EmotionTagParser {
    private val tagRegex = Regex("^\\[emo:\\s*([A-Za-z]+)\\s*]\\s*(.*)$", RegexOption.DOT_MATCHES_ALL)

    fun parse(raw: String): ParsedReply {
        val trimmed = raw.trim()
        val match = tagRegex.matchEntire(trimmed) ?: return ParsedReply(Emotion.NEUTRAL, trimmed)
        val emotion = runCatching { Emotion.valueOf(match.groupValues[1].uppercase()) }
            .getOrDefault(Emotion.NEUTRAL)
        return ParsedReply(emotion, match.groupValues[2].trim())
    }
}
