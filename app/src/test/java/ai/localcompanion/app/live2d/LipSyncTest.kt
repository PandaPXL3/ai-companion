package ai.localcompanion.app.live2d

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.sqrt

class LipSyncTest {
    @Test
    fun silence_keeps_mouth_closed() {
        val lipSync = LipSync()
        // Silence (0 rms) should keep mouth closed
        assertEquals(0f, lipSync.update(0f), 0.001f)
        assertTrue(lipSync.mouthOpen == 0f)
    }

    @Test
    fun loud_sample_opens_mouth_towards_one() {
        val lipSync = LipSync(gain = 10f)  // Higher gain to make mouth open more
        // Loud sample should open mouth
        val result = lipSync.update(0.5f)
        assertTrue(result > 0f)
        assertTrue(result < 1f || result == 1f)
    }

    @Test
    fun smoothing_rises_gradually_first_update_below_target() {
        val lipSync = LipSync(attack = 0.5f, release = 0.12f)
        // Start with mouth closed, apply a target that's above current
        // First update should move mouthOpen toward target
        val result = lipSync.update(0.8f)
        // With attack=0.5, target should be min(1f, 0.8*3)=1f
        // mouthOpen should be 0 + (1 - 0) * 0.5 = 0.5
        assertEquals(0.5f, result, 0.001f)
    }

    @Test
    fun release_falls_slower_than_attack_rises() {
        val lipSync = LipSync(attack = 0.5f, release = 0.12f)
        // Open mouth to some value
        lipSync.update(0.8f)  // mouthOpen goes to ~0.5
        val opened = lipSync.mouthOpen
        
        // Now apply silence - target = 0, so mouth should close using release
        lipSync.update(0f)
        // mouthOpen should have decreased but not to 0 immediately
        assertTrue(lipSync.mouthOpen < opened)
        assertTrue(lipSync.mouthOpen > 0f)
    }

    @Test
    fun rmsOf_empty_is_zero() {
        assertEquals(0f, rmsOf(shortArrayOf()), 0.001f)
    }

    @Test
    fun rmsOf_constant_half_scale_samples_approx_half() {
        // Half-scale samples: 16384 (which is 32768/2)
        val halfScale = shortArrayOf(16384, 16384, 16384, 16384)
        val result = rmsOf(halfScale)
        // RMS of normalized 0.5 samples should be ~0.5
        assertTrue(result >= 0.4f && result <= 0.6f)
    }
}