package ai.localcompanion.app.models

/**
 * Downloadable model packs. The APK ships with no weights; packs are fetched
 * after install like game asset packs — resumable and SHA-256 verified (the
 * downloader lands with the LEAP wiring slice).
 */
data class ModelPack(
    val id: String,
    val title: String,
    val description: String,
    val approxSizeMb: Int,
    val required: Boolean
)

enum class PackStatus { NOT_DOWNLOADED, DOWNLOADING, READY }

object ModelPacks {
    val LFM = ModelPack(
        id = "lfm",
        title = "Her brain — LFM2.5-1.2B",
        description = "The language model. Required for chat.",
        approxSizeMb = 731,
        required = true
    )
    val VOICE = ModelPack(
        id = "voice",
        title = "Voice — Kokoro + Moonshine",
        description = "Her voice (TTS) and ears (STT for audio mode). Skip for text-only.",
        approxSizeMb = 150,
        required = false
    )
    val ALL: List<ModelPack> = listOf(LFM, VOICE)
}
