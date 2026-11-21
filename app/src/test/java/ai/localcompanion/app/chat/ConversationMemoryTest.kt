package ai.localcompanion.app.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMemoryTest {

    @Test
    fun recent_returns_last_n_in_order() {
        val memory = ConversationMemory(3)
        memory.add(Turn(true, "Hello"))
        memory.add(Turn(false, "Hi there"))
        memory.add(Turn(true, "How are you?"))
        memory.add(Turn(false, "I'm fine"))
        memory.add(Turn(true, "Good"))

        val recent = memory.recent()
        assertEquals(3, recent.size)
        assertEquals("How are you?", recent[0].text)
        assertEquals("I'm fine", recent[1].text)
        assertEquals("Good", recent[2].text)
    }

    @Test
    fun needsSummarize_false_at_limit_true_above() {
        val memory = ConversationMemory(3)
        repeat(3) { memory.add(Turn(true, "t$it")) }
        assertFalse(memory.needsSummarize())
        memory.add(Turn(false, "extra"))
        assertTrue(memory.needsSummarize())
    }

    @Test
    fun applySummary_stores_summary_and_trims_turns() {
        val memory = ConversationMemory(3)
        repeat(5) { memory.add(Turn(true, "t$it")) }
        memory.applySummary("Test summary")
        assertEquals("Test summary", memory.summary)
        assertEquals(3, memory.recent().size)
        assertEquals("t2", memory.recent().first().text)
        assertFalse(memory.needsSummarize())
    }

    @Test
    fun buildPromptContext_includes_persona_summary_and_turns() {
        val memory = ConversationMemory(3)
        memory.add(Turn(true, "Hello"))
        memory.add(Turn(false, "Hi"))
        memory.applySummary("Earlier chat summary")

        val context = memory.buildPromptContext("System: persona")
        assertTrue(context.startsWith("System: persona"))
        assertTrue(context.contains("Summary of earlier conversation: Earlier chat summary"))
        assertTrue(context.contains("User: Hello"))
        assertTrue(context.contains("Her: Hi"))
    }

    @Test
    fun buildPromptContext_omits_summary_section_when_blank() {
        val memory = ConversationMemory(3)
        memory.add(Turn(true, "Hello"))
        val context = memory.buildPromptContext("System: persona")
        assertFalse(context.contains("Summary of earlier conversation:"))
    }
}
