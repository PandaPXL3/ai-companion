package ai.localcompanion.app.models

/** One downloadable file inside a pack, pinned by size and SHA-256. */
data class PackFile(
    val url: String,
    val fileName: String,
    /** True for .tar.bz2 archives that are extracted after download. */
    val archive: Boolean,
    val sizeBytes: Long,
    val sha256: String
)

/**
 * Downloadable model packs. The APK ships with no weights; packs are fetched
 * after install like game asset packs — resumable and SHA-256 verified.
 * Hashes below were computed from the actual upstream files (2026-11-21).
 */
data class ModelPack(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean,
    val files: List<PackFile>
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
    val approxSizeMb: Int get() = (totalBytes / 1_000_000).toInt()
}

enum class PackStatus { NOT_DOWNLOADED, DOWNLOADING, READY }

object ModelPacks {
    val LFM = ModelPack(
        id = "lfm",
        title = "Her brain — LFM2.5-1.2B",
        description = "The language model (Q4_K_M). Required for chat.",
        required = true,
        files = listOf(
            PackFile(
                url = "https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct-GGUF/resolve/main/LFM2.5-1.2B-Instruct-Q4_K_M.gguf",
                fileName = "LFM2.5-1.2B-Instruct-Q4_K_M.gguf",
                archive = false,
                sizeBytes = 730_895_168L,
                sha256 = "b1b3de114215d9507409a662a501a631095a479a419584e8a2ded6304b19b4f5"
            )
        )
    )
    val VOICE = ModelPack(
        id = "voice",
        title = "Voice — Kokoro + Moonshine",
        description = "Her voice (Kokoro TTS) and ears (Moonshine STT for audio mode). Skip for text-only.",
        required = false,
        files = listOf(
            PackFile(
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2",
                fileName = "kokoro-en-v0_19.tar.bz2",
                archive = true,
                sizeBytes = 319_625_534L,
                sha256 = "912804855a04745fa77a30be545b3f9a5d15c4d66db00b88cbcd4921df605ac7"
            ),
            PackFile(
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-moonshine-tiny-en-int8.tar.bz2",
                fileName = "sherpa-onnx-moonshine-tiny-en-int8.tar.bz2",
                archive = true,
                sizeBytes = 107_600_538L,
                sha256 = "d5fe6ec4334fef36255b2a4010412cad4c007e33103fec62fb5d17cad88086f2"
            ),
            PackFile(
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx",
                fileName = "silero_vad.onnx",
                archive = false,
                sizeBytes = 643_854L,
                sha256 = "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6"
            )
        )
    )
    val ALL: List<ModelPack> = listOf(LFM, VOICE)
    fun byId(id: String): ModelPack? = ALL.firstOrNull { it.id == id }
}
