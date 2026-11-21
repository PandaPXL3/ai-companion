package ai.localcompanion.app.live2d

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Maps TTS audio amplitude to a Live2D mouth parameter (ParamMouthOpenY, 0..1).
 * Fast attack so syllables open the mouth, slow release so it doesn't jitter shut.
 */
class LipSync(
    private val attack: Float = 0.5f,
    private val release: Float = 0.12f,
    private val gate: Float = 0.02f,
    private val gain: Float = 3.0f
) {
    var mouthOpen: Float = 0f
        private set

    fun update(rms: Float): Float {
        val target = if (rms < gate) 0f else min(1f, rms * gain)
        mouthOpen += if (target > mouthOpen) {
            (target - mouthOpen) * attack
        } else {
            (target - mouthOpen) * release
        }
        return mouthOpen
    }

    fun reset() {
        mouthOpen = 0f
    }
}

/** Root-mean-square of 16-bit PCM samples, normalized to 0..1. */
fun rmsOf(samples: ShortArray): Float {
    if (samples.isEmpty()) return 0f
    var sumOfSquares = 0.0
    for (sample in samples) {
        val normalized = sample / 32768.0
        sumOfSquares += normalized * normalized
    }
    return sqrt(sumOfSquares / samples.size).toFloat()
}
