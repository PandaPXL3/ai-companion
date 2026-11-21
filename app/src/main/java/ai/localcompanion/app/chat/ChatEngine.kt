package ai.localcompanion.app.chat

import ai.liquid.leap.Conversation
import ai.liquid.leap.LeapInferenceEngine
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.message.MessageResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.io.File

/**
 * The on-device brain: LEAP SDK running the downloaded LFM pack.
 *
 * One [ModelRunner] + one [Conversation] for the whole session. The
 * conversation is created with the persona system prompt; generation
 * streams [MessageResponse.Chunk] text, which the UI renders live and
 * finally runs through [EmotionTagParser].
 */
class ChatEngine {

    private var runner: ModelRunner? = null
    private var conversation: Conversation? = null

    val isLoaded: Boolean get() = conversation != null

    suspend fun load(modelFile: File, systemPrompt: String) {
        if (conversation != null) return
        val loaded = LeapInferenceEngine.loadModel(modelFile.absolutePath)
        runner = loaded
        conversation = loaded.createConversation(systemPrompt)
    }

    /** Streaming reply text for one user message. */
    fun generate(userText: String): Flow<String> {
        val active = conversation ?: error("ChatEngine not loaded")
        return active.generateResponse(userText).mapNotNull { response ->
            when (response) {
                is MessageResponse.Chunk -> response.text
                is MessageResponse.Error -> throw response.throwable
                else -> null
            }
        }
    }

    /**
     * One-shot generation on a throwaway conversation (used right after
     * onboarding to expand the user's short personality description into a
     * full persona). Returns null on any failure — expansion is best-effort.
     */
    suspend fun generateOnce(prompt: String): String? {
        val activeRunner = runner ?: return null
        return try {
            val oneShot = activeRunner.createConversation(null)
            val out = StringBuilder()
            oneShot.generateResponse(prompt).collect { response ->
                when (response) {
                    is MessageResponse.Chunk -> out.append(response.text)
                    is MessageResponse.Error -> return null
                    else -> Unit
                }
            }
            out.toString().trim().ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun unload() {
        runner?.unload()
        runner = null
        conversation = null
    }
}
