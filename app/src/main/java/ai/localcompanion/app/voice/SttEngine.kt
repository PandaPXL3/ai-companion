package ai.localcompanion.app.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Her ears: Moonshine Tiny (int8) + Silero VAD via sherpa-onnx, on-device.
 *
 * Loaded ONLY in audio mode and released when it ends (the design rule:
 * zero STT cost in text mode). [listenOnce] records 16 kHz mono, lets the
 * VAD cut the utterance, and decodes the first complete speech segment.
 */
class SttEngine {

    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null

    @Volatile
    private var listening = false

    val isLoaded: Boolean get() = recognizer != null

    fun load(voiceDir: File) {
        if (recognizer != null) return
        val moonshineDir = File(voiceDir, "sherpa-onnx-moonshine-tiny-en-int8")
        recognizer = OfflineRecognizer(
            config = OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = File(moonshineDir, "preprocess.onnx").absolutePath,
                        encoder = File(moonshineDir, "encode.int8.onnx").absolutePath,
                        uncachedDecoder = File(moonshineDir, "uncached_decode.int8.onnx").absolutePath,
                        cachedDecoder = File(moonshineDir, "cached_decode.int8.onnx").absolutePath
                    ),
                    tokens = File(moonshineDir, "tokens.txt").absolutePath,
                    numThreads = 2
                )
            )
        )
        vad = Vad(
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = File(voiceDir, "silero_vad.onnx").absolutePath,
                    minSilenceDuration = 0.6f,
                    maxSpeechDuration = 20.0f
                ),
                sampleRate = SAMPLE_RATE
            )
        )
    }

    /**
     * Records until the VAD closes one utterance (or [stop] is called /
     * ~15 s pass without speech), then returns the transcript or null.
     * Requires RECORD_AUDIO permission — the caller checks it.
     */
    @SuppressLint("MissingPermission")
    suspend fun listenOnce(): String? = withContext(Dispatchers.IO) {
        val activeRecognizer = recognizer ?: return@withContext null
        val activeVad = vad ?: return@withContext null
        val window = activeVad.config.sileroVadModelConfig.windowSize // 512 samples
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, window * 2 * 4)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return@withContext null
        }
        listening = true
        try {
            recorder.startRecording()
            val buffer = ShortArray(window)
            var silentWindows = 0
            while (listening) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                val floats = FloatArray(read) { i -> buffer[i] / 32768f }
                activeVad.acceptWaveform(floats)
                if (!activeVad.empty()) {
                    val segment = activeVad.front()
                    activeVad.pop()
                    return@withContext decode(activeRecognizer, segment.samples)
                }
                if (!activeVad.isSpeechDetected()) {
                    // ~15 s of no speech at all: give up quietly
                    if (++silentWindows > 450) return@withContext null
                } else {
                    silentWindows = 0
                }
            }
            null
        } finally {
            listening = false
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private fun decode(recognizer: OfflineRecognizer, samples: FloatArray): String? {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim().ifBlank { null }
        } finally {
            stream.release()
        }
    }

    fun stop() {
        listening = false
    }

    fun release() {
        stop()
        recognizer?.release()
        recognizer = null
        vad?.release()
        vad = null
    }

    private companion object {
        const val SAMPLE_RATE = 16000
    }
}
