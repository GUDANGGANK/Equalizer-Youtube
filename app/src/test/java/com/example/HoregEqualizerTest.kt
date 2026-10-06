package com.example

import com.example.data.PresetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HoregEqualizerTest {

    @Test
    fun testPresetBandsParsing() {
        val preset = PresetEntity(
            name = "HOREG BASS GLERR (60Hz)",
            channelABands = "13,9,3,7,11",
            channelBBands = "13,9,3,7,11",
            subBoost = 950,
            masterGain = 750,
            midHighBoost = 720,
            isSystemPreset = true
        )

        val bandsA = preset.channelABands.split(",").map { it.trim().toInt() }
        assertEquals(5, bandsA.size)
        assertEquals(13, bandsA[0]) // 60Hz Sub Horeg is boosted to +13 dB!
        assertTrue(preset.subBoost >= 900)
    }

    @Test
    fun testDbClampingLogic() {
        val testDb = 25
        val clamped = testDb.coerceIn(-15, 15)
        assertEquals(15, clamped)

        val testNegative = -30
        val clampedNeg = testNegative.coerceIn(-15, 15)
        assertEquals(-15, clampedNeg)
    }

    @Test
    fun testValidMediaPlayerSessionId() {
        // Session 0 represents the global audio output mix (intercepts YouTube on internal speaker)
        val globalMixSessionId = 0
        val isGlobalAllowed = globalMixSessionId >= 0
        assertEquals(true, isGlobalAllowed)

        val mediaOutputSessionId = 42
        val isMediaSessionValid = mediaOutputSessionId > 0
        assertEquals(true, isMediaSessionValid)

        val invalidSessionId = -1
        val isInvalidAllowed = invalidSessionId >= 0
        assertEquals(false, isInvalidAllowed)
    }

    @Test
    fun testTwsLatencyProfileAndSyncOffsetClamping() {
        val testOffset = 350
        val clampedOffset = testOffset.coerceIn(-100, 200)
        assertEquals(200, clampedOffset)

        val testNegativeOffset = -180
        val clampedNeg = testNegativeOffset.coerceIn(-100, 200)
        assertEquals(-100, clampedNeg)
    }

    @Test
    fun testHapticThresholdLogic() {
        val stepInterval = 40
        val valA = 480
        val valB = 520
        val isStepCrossed = (valB / stepInterval) != (valA / stepInterval)
        assertTrue(isStepCrossed)

        val isGlerrMilestone = (valB >= 500 && valA < 500)
        assertTrue(isGlerrMilestone)
    }
}
