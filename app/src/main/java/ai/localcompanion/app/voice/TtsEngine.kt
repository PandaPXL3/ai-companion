package ai.localcompanion.app.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import ai.localcompanion.app.live2d.LipSync
import ai.localcompanion.app.live2d.rmsOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Her voice: Kokoro-82M via sherpa-onnx, fully on-device.
 *
 * Synthesis streams through [OfflineTts.generateWithCallback]; each chunk is
 * written to an [AudioTrack] as 16-bit PCM and its RMS feeds [LipSync], so
 * the Live2D bridge can read [mouthOpen] for ParamMouthOpenY once Cubism
 * lands. Loads lazily from the extracted voice pack and only when needed.
 */
class TtsEngine {

    private var tts: OfflineTts? = null

    @Volatile
    private var stopped = false

    private val lipSync = LipSync()
    private val _mouthOpen = MutableStateFlow(0f)

    /** Smoothed mouth-open value 0..1, synced to the audio actually playing. */
    val mouthOpen: StateFlow<Float> = _mouthOpen

    val isLoaded: Boolean get() = tts != null

    fun load(voiceDir: File) {
        if (tts != null) return
        val kokoroDir = File(voiceDir, "kokoro-en-v0_19")
        tts = OfflineTts(
            config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = File(kokoroDir, "model.onnx").absolutePath,
                        voices = File(kokoroDir, "voices.bin").absolutePath,
                        tokens = File(kokoroDir, "tokens.txt").absolutePath,
                        dataDir = File(kokoroDir, "espeak-ng-data").absolutePath
                    ),
                    numThreads = 2
                )
            )
        )
    }

    /** Synthesizes and plays [text]; returns when playback has finished. */
    suspend fun speak(text: String, volume: Float) = withContext(Dispatchers.IO) {
        val engine = tts ?: return@withContext
        stopped = false
        val rate = engine.sampleRate()
        val minBuffer = AudioTrack.getMinBufferSize(
            rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, rate))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.setVolume(volume.coerceIn(0f, 1f))
            track.play()
            var writtenFrames = 0
            engine.generateWithCallback(text, sid = 0, speed = 1.0f) { samples ->
                if (stopped) return@generateWithCallback 0
                val shorts = ShortArray(samples.size) { i ->
                    (samples[i].coerceIn(-1f, 1f) * 32767).toInt().toShort()
                }
                var offset = 0
                while (offset < shorts.size && !stopped) {
                    val n = track.write(shorts, offset, shorts.size - offset)
                    if (n <= 0) break
                    offset += n
                }
                writtenFrames += offset
                _mouthOpen.value = lipSync.update(rmsOf(shorts))
                if (stopped) 0 else 1
            }
            while (!stopped && track.playbackHeadPosition < writtenFrames) {
                Thread.sleep(20)
            }
        } finally {
            runCatching { track.stop() }
            track.release()
            lipSync.reset()
            _mouthOpen.value = 0f
        }
    }

    fun stop() {
        stopped = true
    }

    fun release() {
        stop()
        tts?.release()
        tts = null
    }
}
