package ai.localcompanion.app.profile

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PersonaPromptBuilderTest {

    private val profile = UserProfile(
        userName = "Manoj",
        userAge = 24,
        companionName = "Aira",
        personalityShort = "Cheerful, teasing, loves books"
    )

    @Test
    fun `system prompt names the companion and the user`() {
        val prompt = PersonaPromptBuilder.buildSystemPrompt(profile)
        assertTrue(prompt.contains("Aira"))
        assertTrue(prompt.contains("Manoj"))
    }

    @Test
    fun `system prompt falls back to the short description when no expansion exists`() {
        val prompt = PersonaPromptBuilder.buildSystemPrompt(profile)
        assertTrue(prompt.contains("Cheerful, teasing, loves books"))
    }

    @Test
    fun `system prompt prefers the expanded persona when present`() {
        val expanded = profile.copy(personaPrompt = "You are Aira, a book-obsessed ray of sunshine.")
        val prompt = PersonaPromptBuilder.buildSystemPrompt(expanded)
        assertTrue(prompt.contains("book-obsessed ray of sunshine"))
        assertFalse(prompt.contains("Cheerful, teasing, loves books"))
    }

    @Test
    fun `system prompt always carries the emotion tag contract`() {
        val prompt = PersonaPromptBuilder.buildSystemPrompt(profile)
        for (tag in listOf("neutral", "happy", "shy", "sad", "pout", "surprised", "thinking", "sleepy")) {
            assertTrue("missing tag $tag", prompt.contains("[emo:$tag]"))
        }
    }

    @Test
    fun `system prompt carries the AI honesty rule`() {
        val prompt = PersonaPromptBuilder.buildSystemPrompt(profile)
        assertTrue(prompt.contains("not a real person"))
    }

    @Test
    fun `expansion prompt includes name, description and user`() {
        val prompt = PersonaPromptBuilder.buildExpansionPrompt(profile)
        assertTrue(prompt.contains("Aira"))
        assertTrue(prompt.contains("Cheerful, teasing, loves books"))
        assertTrue(prompt.contains("Manoj"))
        assertTrue(prompt.contains("second person"))
    }
}
