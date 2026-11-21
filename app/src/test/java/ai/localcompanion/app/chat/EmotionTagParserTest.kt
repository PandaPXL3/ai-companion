package ai.localcompanion.app.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionTagParserTest {

    @Test
    fun `happy tag parsed and stripped`() {
        val result = EmotionTagParser.parse("[emo:happy] Hello world")
        assertEquals(Emotion.HAPPY, result.emotion)
        assertEquals("Hello world", result.spokenText)
    }

    @Test
    fun `no tag defaults neutral`() {
        val result = EmotionTagParser.parse("Just regular text")
        assertEquals(Emotion.NEUTRAL, result.emotion)
        assertEquals("Just regular text", result.spokenText)
    }

    @Test
    fun `unknown emotion maps to neutral and tag is stripped`() {
        val result = EmotionTagParser.parse("[emo:confused] Hello")
        assertEquals(Emotion.NEUTRAL, result.emotion)
        assertEquals("Hello", result.spokenText)
    }

    @Test
    fun `case-insensitive tag`() {
        assertEquals(Emotion.HAPPY, EmotionTagParser.parse("[emo:Happy] text").emotion)
        assertEquals(Emotion.HAPPY, EmotionTagParser.parse("[EMO:HAPPY] text").emotion)
    }

    @Test
    fun `whitespace handling`() {
        val result = EmotionTagParser.parse("  [emo:sad]   text   ")
        assertEquals(Emotion.SAD, result.emotion)
        assertEquals("text", result.spokenText)
        val tagOnly = EmotionTagParser.parse("[emo:shy]")
        assertEquals(Emotion.SHY, tagOnly.emotion)
        assertEquals("", tagOnly.spokenText)
    }

    @Test
    fun `bracketed text that is not an emo tag is kept`() {
        val result = EmotionTagParser.parse("[note] keep me")
        assertEquals(Emotion.NEUTRAL, result.emotion)
        assertEquals("[note] keep me", result.spokenText)
    }
}
