package ai.localcompanion.app.chat

data class Turn(val fromUser: Boolean, val text: String)

/**
 * Short-term conversation memory. Turns accumulate until they exceed
 * [maxRecentTurns]; then the caller asks the LLM to summarize the older
 * part and calls [applySummary], which folds them into [summary] and
 * keeps only the recent window. The prompt context therefore stays
 * bounded (battery + context size) while long-term facts survive in
 * the summary.
 */
class ConversationMemory(private val maxRecentTurns: Int = 12) {
    private val turns = mutableListOf<Turn>()

    var summary: String = ""
        private set

    fun add(turn: Turn) {
        turns.add(turn)
    }

    /** The last [maxRecentTurns] turns, oldest first. */
    fun recent(): List<Turn> = turns.takeLast(maxRecentTurns)

    fun needsSummarize(): Boolean = turns.size > maxRecentTurns

    fun applySummary(newSummary: String) {
        summary = newSummary
        val keep = turns.takeLast(maxRecentTurns)
        turns.clear()
        turns.addAll(keep)
    }

    fun buildPromptContext(personaCard: String): String {
        val parts = mutableListOf(personaCard)
        if (summary.isNotBlank()) {
            parts.add("Summary of earlier conversation: $summary")
        }
        for (turn in recent()) {
            parts.add("${if (turn.fromUser) "User" else "Her"}: ${turn.text}")
        }
        return parts.joinToString("\n")
    }
}
