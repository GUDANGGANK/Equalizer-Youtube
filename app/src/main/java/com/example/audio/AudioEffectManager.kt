package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val label: String
)

data class BluetoothStatus(
    val isConnected: Boolean,
    val deviceName: String,
    val isLowLatencyActive: Boolean,
    val audioType: String
)

enum class TwsLatencyProfile(val title: String, val latencyMs: Int, val desc: String) {
    ULTRA_LOW("Ultra-Low (~40ms)", 40, "Khusus gaming & video YouTube tanpa delay bibir (lip-sync presisi)"),
    BALANCED("Balanced (~80ms)", 80, "Stabil untuk TWS harian, podcast & streaming musik"),
    HIGH_STABILITY("High Stability (~150ms)", 150, "Buffer besar anti drop/stutter di area sinyal padat")
}

class AudioEffectManager(private val context: Context) {
    companion object {
        private const val TAG = "AudioEffectManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val scope = CoroutineScope(Dispatchers.Main)

    // Managed Audio Session ID (generated per modern Android audio best practices)
    val defaultSessionId: Int by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                audioManager.generateAudioSessionId()
            } catch (e: Exception) {
                1001
            }
        } else {
            1001
        }
    }

    // Data structures for session effects
    private data class SessionHolder(
        val sessionId: Int,
        var equalizer: Equalizer? = null,
        var bassBoost: BassBoost? = null,
        var loudnessEnhancer: LoudnessEnhancer? = null,
        var virtualizer: Virtualizer? = null
    )

    private val activeSessions = mutableMapOf<Int, SessionHolder>()
    val toneGenerator = AudioToneGenerator()

    // Real-time Android Hardware Visualizer for YouTube audio sync
    private var hardwareVisualizer: Visualizer? = null
    private var visualizerSessionId = 0
    private var lastRealAudioTimestamp = 0L

    // State flows
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _isStereoLinked = MutableStateFlow(true)
    val isStereoLinked: StateFlow<Boolean> = _isStereoLinked.asStateFlow()

    private val _bands = MutableStateFlow<List<BandInfo>>(emptyList())
    val bands: StateFlow<List<BandInfo>> = _bands.asStateFlow()

    // Band levels in dB (-15 to +15)
    private val _channelABands = MutableStateFlow<List<Int>>(listOf(13, 9, 3, 7, 11))
    val channelABands: StateFlow<List<Int>> = _channelABands.asStateFlow()

    private val _channelBBands = MutableStateFlow<List<Int>>(listOf(13, 9, 3, 7, 11))
    val channelBBands: StateFlow<List<Int>> = _channelBBands.asStateFlow()

    // 60Hz Sub Boost (0 to 1000)
    private val _subBoost = MutableStateFlow(950)
    val subBoost: StateFlow<Int> = _subBoost.asStateFlow()

    // Master Gain Pre-Amp (0 to 1000, maps to 0 to 1200 mB)
    private val _masterGain = MutableStateFlow(750)
    val masterGain: StateFlow<Int> = _masterGain.asStateFlow()

    // Mid-High Clarity (0 to 1000)
    private val _midHighBoost = MutableStateFlow(720)
    val midHighBoost: StateFlow<Int> = _midHighBoost.asStateFlow()

    // Anti-Delay TWS Bluetooth mode
    private val _lowLatencyTwsEnabled = MutableStateFlow(true)
    val lowLatencyTwsEnabled: StateFlow<Boolean> = _lowLatencyTwsEnabled.asStateFlow()

    private val _twsLatencyProfile = MutableStateFlow(TwsLatencyProfile.ULTRA_LOW)
    val twsLatencyProfile: StateFlow<TwsLatencyProfile> = _twsLatencyProfile.asStateFlow()

    private val _audioSyncOffsetMs = MutableStateFlow(0) // -100ms to +200ms
    val audioSyncOffsetMs: StateFlow<Int> = _audioSyncOffsetMs.asStateFlow()

    private val _monoSummingEnabled = MutableStateFlow(false)
    val monoSummingEnabled: StateFlow<Boolean> = _monoSummingEnabled.asStateFlow()

    // Bluetooth & Output Status
    private val _bluetoothStatus = MutableStateFlow(
        BluetoothStatus(
            isConnected = false,
            deviceName = "Speaker HP Bawaan",
            isLowLatencyActive = true,
            audioType = "Built-in Speaker"
        )
    )
    val bluetoothStatus: StateFlow<BluetoothStatus> = _bluetoothStatus.asStateFlow()

    // Specifically track the active media player output session ID (e.g. from YouTube)
    private val _activeMediaSessionId = MutableStateFlow<Int?>(null)
    val activeMediaSessionId: StateFlow<Int?> = _activeMediaSessionId.asStateFlow()

    // Permission and Background Execution status
    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _isBatteryExempt = MutableStateFlow(false)
    val isBatteryExempt: StateFlow<Boolean> = _isBatteryExempt.asStateFlow()

    private val _isYouTubeAudioActive = MutableStateFlow(false)
    val isYouTubeAudioActive: StateFlow<Boolean> = _isYouTubeAudioActive.asStateFlow()

    // VU Meter levels (0.0 to 1.0)
    private val _vuLevelA = MutableStateFlow(0f)
    val vuLevelA: StateFlow<Float> = _vuLevelA.asStateFlow()

    private val _vuLevelB = MutableStateFlow(0f)
    val vuLevelB: StateFlow<Float> = _vuLevelB.asStateFlow()

    private val _peakLevelA = MutableStateFlow(0f)
    val peakLevelA: StateFlow<Float> = _peakLevelA.asStateFlow()

    private val _peakLevelB = MutableStateFlow(0f)
    val peakLevelB: StateFlow<Float> = _peakLevelB.asStateFlow()

    // Spectrum bands for real-time visualization (5 bands legacy)
    private val _spectrumLevels = MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f))
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()

    // High-resolution 24-band real-time logarithmic spectrum analyzer (0.0 to 1.0)
    private val _spectrumBands24 = MutableStateFlow(List(24) { 0f })
    val spectrumBands24: StateFlow<List<Float>> = _spectrumBands24.asStateFlow()

    // Peak hold points for 24-band spectrum
    private val _spectrumPeaks24 = MutableStateFlow(List(24) { 0f })
    val spectrumPeaks24: StateFlow<List<Float>> = _spectrumPeaks24.asStateFlow()

    private var vuMeterFallbackJob: Job? = null

    init {
        initBands()
        setupDefaultSessions()
        startAudioRouteMonitoring()
        startAudioPlaybackMonitoring()
        checkPermissions()
        startVuMeterEngine()

        // Hook tone generator amplitude to VU meters when user auditions test audio
        toneGenerator.onAmplitudeUpdate = { left, right ->
            if (toneGenerator.isPlaying) {
                _vuLevelA.value = left
                _vuLevelB.value = right
                if (left > _peakLevelA.value) _peakLevelA.value = left
                if (right > _peakLevelB.value) _peakLevelB.value = right
                _isYouTubeAudioActive.value = true
            }
        }
    }

    fun checkPermissions() {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _hasAudioPermission.value = hasAudio

        val isExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
        _isBatteryExempt.value = isExempt

        if (hasAudio) {
            val target = if (visualizerSessionId >= 0) visualizerSessionId else 0
            initHardwareVisualizer(target)
        }
    }

    fun requestIgnoreBatteryOptimizations(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(appSettingsIntent)
                } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Initializes the real-time Android AudioFX Visualizer.
     * Defaults to Session 0 (Global Output Mix) to intercept all external media players
     * including YouTube playing on internal speaker or in the background.
     */
    fun initHardwareVisualizer(sessionId: Int = 0) {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasAudio) {
            Log.d(TAG, "Cannot init visualizer: RECORD_AUDIO permission not granted yet")
            return
        }

        try {
            hardwareVisualizer?.let {
                try { it.enabled = false; it.release() } catch (ignored: Exception) {}
            }
            hardwareVisualizer = null

            val targetSession = if (sessionId >= 0) sessionId else 0
            visualizerSessionId = targetSession

            val vis = try {
                Visualizer(targetSession)
            } catch (e: Exception) {
                Log.w(TAG, "Visualizer on session $targetSession failed, falling back to session 0", e)
                Visualizer(0)
            }

            val range = Visualizer.getCaptureSizeRange()
            vis.captureSize = range[1].coerceAtMost(256)

            // Normalized scaling ensures low volume audio on internal phone speakers is captured
            try {
                vis.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            } catch (ignored: Exception) {}

            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    if (!_isEnabled.value || waveform == null || waveform.isEmpty()) return
                    if (toneGenerator.isPlaying) return // Built-in tester handles its own meters

                    val len = waveform.size
                    val mid = len / 2
                    var sumL = 0.0
                    var sumR = 0.0

                    // Left channel waveform RMS
                    for (i in 0 until mid) {
                        val sample = (waveform[i].toInt() and 0xFF) - 128
                        sumL += sample * sample
                    }
                    // Right channel waveform RMS
                    for (i in mid until len) {
                        val sample = (waveform[i].toInt() and 0xFF) - 128
                        sumR += sample * sample
                    }

                    val rawL = Math.sqrt(sumL / mid.toDouble()).toFloat()
                    val rawR = Math.sqrt(sumR / (len - mid).toDouble()).toFloat()

                    // Enhanced sensitivity specifically for internal speakers
                    val rmsL = (rawL / 35.0f).coerceIn(0f, 1f)
                    val rmsR = (rawR / 35.0f).coerceIn(0f, 1f)

                    if (rawL > 1.0f || rawR > 1.0f) {
                        lastRealAudioTimestamp = System.currentTimeMillis()
                        _isYouTubeAudioActive.value = true
                    } else if (System.currentTimeMillis() - lastRealAudioTimestamp > 1800) {
                        _isYouTubeAudioActive.value = false
                    }

                    // Apply current master gain and sub-boost amplification to visualizer
                    val gainScale = 1.0f + (_masterGain.value / 1000f) * 0.45f
                    val targetA = (rmsL * gainScale).coerceIn(0f, 1f)
                    val targetB = (rmsR * gainScale).coerceIn(0f, 1f)

                    // Fast attack, smooth decay
                    _vuLevelA.value = (_vuLevelA.value * 0.25f + targetA * 0.75f).coerceIn(0f, 1f)
                    _vuLevelB.value = (_vuLevelB.value * 0.25f + targetB * 0.75f).coerceIn(0f, 1f)

                    if (_vuLevelA.value > _peakLevelA.value) {
                        _peakLevelA.value = _vuLevelA.value
                    } else {
                        _peakLevelA.value = (_peakLevelA.value - 0.035f).coerceAtLeast(_vuLevelA.value)
                    }

                    if (_vuLevelB.value > _peakLevelB.value) {
                        _peakLevelB.value = _vuLevelB.value
                    } else {
                        _peakLevelB.value = (_peakLevelB.value - 0.035f).coerceAtLeast(_vuLevelB.value)
                    }
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    if (!_isEnabled.value || fft == null || fft.isEmpty() || toneGenerator.isPlaying) return

                    val n = fft.size / 2
                    val mags = FloatArray(n)
                    for (k in 0 until n) {
                        val r = fft[2 * k].toFloat()
                        val im = fft[2 * k + 1].toFloat()
                        mags[k] = (Math.hypot(r.toDouble(), im.toDouble()).toFloat() / 55.0f).coerceIn(0f, 1.5f)
                    }

                    // 1. High-resolution 24 logarithmic frequency bands
                    val cur24 = _spectrumBands24.value
                    val curPeaks = _spectrumPeaks24.value.toMutableList()
                    val new24 = FloatArray(24)

                    for (i in 0 until 24) {
                        val startFrac = Math.pow(i / 24.0, 2.1)
                        val endFrac = Math.pow((i + 1) / 24.0, 2.1)
                        val startIdx = (startFrac * (n - 1)).toInt().coerceIn(0, n - 1)
                        val endIdx = (endFrac * (n - 1)).toInt().coerceIn(startIdx + 1, n)

                        var sum = 0f
                        var count = 0
                        for (bin in startIdx until endIdx) {
                            sum += mags[bin]
                            count++
                        }
                        val avg = if (count > 0) sum / count else mags[startIdx]

                        // Real-time EQ influence preview
                        val boost = when {
                            i <= 2 -> 1.0f + (_subBoost.value / 1000f) * 0.95f
                            i in 3..6 -> 1.0f + (_subBoost.value / 1000f) * 0.55f
                            i in 7..14 -> 1.0f + (_masterGain.value / 1000f) * 0.45f
                            else -> 1.0f + (_midHighBoost.value / 1000f) * 0.75f
                        }

                        val target = (avg * boost * 1.5f).coerceIn(0f, 1f)
                        val smoothed = (cur24.getOrElse(i) { 0f } * 0.20f + target * 0.80f).coerceIn(0f, 1f)
                        new24[i] = smoothed

                        val p = curPeaks.getOrElse(i) { 0f }
                        curPeaks[i] = if (smoothed > p) smoothed else (p - 0.025f).coerceAtLeast(smoothed)
                    }

                    _spectrumBands24.value = new24.toList()
                    _spectrumPeaks24.value = curPeaks

                    // 2. Backward compatibility 5-band spectrum mapping
                    val b0 = mags.take((n * 0.08).toInt().coerceAtLeast(1)).average().toFloat()
                    val b1 = mags.drop((n * 0.08).toInt()).take((n * 0.15).toInt().coerceAtLeast(1)).average().toFloat()
                    val b2 = mags.drop((n * 0.23).toInt()).take((n * 0.25).toInt().coerceAtLeast(1)).average().toFloat()
                    val b3 = mags.drop((n * 0.48).toInt()).take((n * 0.25).toInt().coerceAtLeast(1)).average().toFloat()
                    val b4 = mags.drop((n * 0.73).toInt()).average().toFloat()

                    val current = _spectrumLevels.value
                    _spectrumLevels.value = listOf(
                        (current[0] * 0.3f + b0.coerceIn(0f, 1f) * 0.7f),
                        (current[1] * 0.3f + b1.coerceIn(0f, 1f) * 0.7f),
                        (current[2] * 0.3f + b2.coerceIn(0f, 1f) * 0.7f),
                        (current[3] * 0.3f + b3.coerceIn(0f, 1f) * 0.7f),
                        (current[4] * 0.3f + b4.coerceIn(0f, 1f) * 0.7f)
                    )
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)

            vis.enabled = true
            hardwareVisualizer = vis
            Log.d(TAG, "Visualizer successfully attached to session $targetSession")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach hardware visualizer to session $sessionId: ${e.message}")
        }
    }

    private fun initBands() {
        try {
            val probeEq = Equalizer(1000, defaultSessionId)
            val numBands = probeEq.numberOfBands.toInt()
            val list = mutableListOf<BandInfo>()
            for (i in 0 until numBands) {
                val centerFreq = probeEq.getCenterFreq(i.toShort()) / 1000 // in Hz
                val label = if (centerFreq >= 1000) {
                    val khz = centerFreq / 1000.0
                    if (khz == khz.toLong().toDouble()) "${khz.toLong()} kHz" else "%.1f kHz".format(khz)
                } else {
                    "$centerFreq Hz"
                }
                list.add(BandInfo(index = i, centerFreqHz = centerFreq, label = label))
            }
            probeEq.release()

            if (list.isNotEmpty()) {
                _bands.value = list
                val defaultList = List(list.size) { idx ->
                    when (idx) {
                        0 -> 13 // 60Hz Sub Horeg
                        1 -> 9  // Mid-bass punch
                        2 -> 3  // Mid
                        3 -> 7  // Presence
                        else -> 11 // High sparkle
                    }
                }
                _channelABands.value = defaultList
                _channelBBands.value = defaultList
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hardware equalizer probe failed, using standard 5-band Horeg defaults", e)
        }

        // Standard 5-band Horeg fallback
        _bands.value = listOf(
            BandInfo(0, 60, "60 Hz"),
            BandInfo(1, 230, "230 Hz"),
            BandInfo(2, 910, "910 Hz"),
            BandInfo(3, 3600, "3.6 kHz"),
            BandInfo(4, 14000, "14 kHz")
        )
        val defaultList = listOf(13, 9, 3, 7, 11)
        _channelABands.value = defaultList
        _channelBBands.value = defaultList
    }

    private fun setupDefaultSessions() {
        // Attach to Session 0 (Global Output Mix) - intercepts all YouTube and internal speaker audio!
        attachSession(0)
        if (defaultSessionId > 0 && defaultSessionId != 0) {
            attachSession(defaultSessionId)
        }
    }

    fun openSession(sessionId: Int, packageName: String? = null) {
        Log.d(TAG, "openSession: media player session ID = $sessionId from package $packageName")
        if (sessionId < 0) return
        _activeMediaSessionId.value = if (sessionId > 0) sessionId else null
        attachSession(sessionId)

        // Attach hardware visualizer to this media player's active output session
        if (_hasAudioPermission.value) {
            initHardwareVisualizer(sessionId)
        }
    }

    fun closeSession(sessionId: Int) {
        Log.d(TAG, "closeSession: media player session ID = $sessionId")
        if (sessionId < 0) return
        if (sessionId != 0) {
            detachSession(sessionId)
        }
        if (_activeMediaSessionId.value == sessionId) {
            _activeMediaSessionId.value = null
        }
        if (visualizerSessionId == sessionId) {
            // Revert back to Global Output Mix (0)
            initHardwareVisualizer(0)
        }
    }

    private fun attachSession(sessionId: Int) {
        if (sessionId < 0) {
            Log.w(TAG, "Rejecting invalid audio session ID $sessionId")
            return
        }
        try {
            if (activeSessions.containsKey(sessionId)) {
                return
            }

            val holder = SessionHolder(sessionId)

            // Equalizer (Priority 1000 so Horeg EQ overrides flat default sound)
            try {
                val eq = Equalizer(1000, sessionId)
                eq.enabled = _isEnabled.value
                holder.equalizer = eq
                Log.d(TAG, "Equalizer successfully attached to session $sessionId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create Equalizer for session $sessionId: ${e.message}")
            }

            // Bass Boost (Priority 1000 for sub-bass punch)
            try {
                val bb = BassBoost(1000, sessionId)
                bb.enabled = _isEnabled.value
                val strength = (_subBoost.value).coerceIn(0, 1000).toShort()
                bb.setStrength(strength)
                holder.bassBoost = bb
                Log.d(TAG, "BassBoost successfully attached to session $sessionId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create BassBoost for session $sessionId: ${e.message}")
            }

            // Loudness Enhancer (Gain Pre-amp)
            try {
                val le = LoudnessEnhancer(sessionId)
                le.enabled = _isEnabled.value
                val gainMb = ((_masterGain.value / 1000f) * 1200f).roundToInt()
                le.setTargetGain(gainMb)
                holder.loudnessEnhancer = le
                Log.d(TAG, "LoudnessEnhancer successfully attached to session $sessionId")
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer note for session $sessionId: ${e.message}")
            }

            // Virtualizer
            try {
                val virt = Virtualizer(1000, sessionId)
                virt.enabled = _isEnabled.value && !_lowLatencyTwsEnabled.value
                if (virt.enabled) {
                    virt.setStrength(300)
                }
                holder.virtualizer = virt
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer note for session $sessionId: ${e.message}")
            }

            activeSessions[sessionId] = holder
            applySettingsToSession(holder)
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching audio effects to session $sessionId", e)
        }
    }

    private fun detachSession(sessionId: Int) {
        activeSessions.remove(sessionId)?.let { holder ->
            try { holder.equalizer?.release() } catch (ignored: Exception) {}
            try { holder.bassBoost?.release() } catch (ignored: Exception) {}
            try { holder.loudnessEnhancer?.release() } catch (ignored: Exception) {}
            try { holder.virtualizer?.release() } catch (ignored: Exception) {}
        }
    }

    private fun applySettingsToSession(holder: SessionHolder) {
        val enabled = _isEnabled.value

        // Equalizer bands
        holder.equalizer?.let { eq ->
            try {
                eq.enabled = enabled
                val numBands = eq.numberOfBands.toInt()
                val bandsA = _channelABands.value
                val bandsB = _channelBBands.value

                for (i in 0 until numBands) {
                    val valA = bandsA.getOrElse(i) { 0 }
                    val valB = bandsB.getOrElse(i) { 0 }
                    val blendedDb = (valA + valB) / 2.0f

                    // Extra sub-boost curve enhancement for 60Hz (band 0)
                    val subExtra = if (i == 0) (_subBoost.value / 1000f) * 4.0f else 0.0f
                    // Mid-high presence enhancement for upper bands
                    val midHighExtra = if (i >= 3) (_midHighBoost.value / 1000f) * 3.0f else 0.0f

                    val totalDb = (blendedDb + subExtra + midHighExtra).coerceIn(-15f, 15f)
                    val millibels = (totalDb * 100f).roundToInt().toShort()

                    val minRange = eq.bandLevelRange[0]
                    val maxRange = eq.bandLevelRange[1]
                    val safeMb = millibels.coerceIn(minRange, maxRange)

                    eq.setBandLevel(i.toShort(), safeMb)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply band levels to session ${holder.sessionId}", e)
            }
        }

        // Bass Boost
        holder.bassBoost?.let { bb ->
            try {
                bb.enabled = enabled
                val strength = (_subBoost.value).coerceIn(0, 1000).toShort()
                bb.setStrength(strength)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply bass boost", e)
            }
        }

        // Master Gain Pre-Amp (LoudnessEnhancer)
        holder.loudnessEnhancer?.let { le ->
            try {
                le.enabled = enabled
                val gainMb = ((_masterGain.value / 1000f) * 1200f).roundToInt()
                le.setTargetGain(gainMb)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply loudness enhancer", e)
            }
        }

        // Virtualizer (Spatializer) - Must be disabled in Low Latency TWS mode
        holder.virtualizer?.let { virt ->
            try {
                if (_lowLatencyTwsEnabled.value || !enabled) {
                    virt.enabled = false
                } else {
                    virt.enabled = true
                    virt.setStrength((_midHighBoost.value / 2).coerceIn(0, 1000).toShort())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to configure virtualizer", e)
            }
        }
    }

    fun applyAllSettings() {
        activeSessions.values.forEach { holder ->
            applySettingsToSession(holder)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        hardwareVisualizer?.let {
            try { it.enabled = enabled } catch (ignored: Exception) {}
        }
        applyAllSettings()
    }

    fun setStereoLinked(linked: Boolean) {
        _isStereoLinked.value = linked
        if (linked) {
            _channelBBands.value = _channelABands.value
            applyAllSettings()
        }
    }

    fun updateChannelABand(bandIndex: Int, levelDb: Int) {
        val current = _channelABands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = levelDb.coerceIn(-15, 15)
            _channelABands.value = current

            if (_isStereoLinked.value) {
                val currentB = _channelBBands.value.toMutableList()
                if (bandIndex in currentB.indices) {
                    currentB[bandIndex] = levelDb.coerceIn(-15, 15)
                    _channelBBands.value = currentB
                }
            }
            applyAllSettings()
        }
    }

    fun updateChannelBBand(bandIndex: Int, levelDb: Int) {
        val current = _channelBBands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = levelDb.coerceIn(-15, 15)
            _channelBBands.value = current

            if (_isStereoLinked.value) {
                val currentA = _channelABands.value.toMutableList()
                if (bandIndex in currentA.indices) {
                    currentA[bandIndex] = levelDb.coerceIn(-15, 15)
                    _channelABands.value = currentA
                }
            }
            applyAllSettings()
        }
    }

    fun setSubBoost(value: Int) {
        _subBoost.value = value.coerceIn(0, 1000)
        applyAllSettings()
    }

    fun setMasterGain(value: Int) {
        _masterGain.value = value.coerceIn(0, 1000)
        applyAllSettings()
    }

    fun setMidHighBoost(value: Int) {
        _midHighBoost.value = value.coerceIn(0, 1000)
        applyAllSettings()
    }

    fun setLowLatencyTwsEnabled(enabled: Boolean) {
        _lowLatencyTwsEnabled.value = enabled
        updateBluetoothStatus()
        applyAllSettings()
    }

    fun setTwsLatencyProfile(profile: TwsLatencyProfile) {
        _twsLatencyProfile.value = profile
        if (profile == TwsLatencyProfile.ULTRA_LOW) {
            _lowLatencyTwsEnabled.value = true
        }
        updateBluetoothStatus()
        applyAllSettings()
    }

    fun setAudioSyncOffsetMs(offset: Int) {
        _audioSyncOffsetMs.value = offset.coerceIn(-100, 200)
    }

    fun setMonoSummingEnabled(enabled: Boolean) {
        _monoSummingEnabled.value = enabled
        applyAllSettings()
    }

    fun playTwsChannelTest(isLeft: Boolean) {
        toneGenerator.playChannelChime(isLeft, defaultSessionId)
    }

    fun refreshBluetoothStatus() {
        updateBluetoothStatus()
    }

    fun applyPreset(
        bandsA: List<Int>,
        bandsB: List<Int>,
        sub: Int,
        gain: Int,
        midHigh: Int
    ) {
        _channelABands.value = bandsA
        _channelBBands.value = bandsB
        _subBoost.value = sub
        _masterGain.value = gain
        _midHighBoost.value = midHigh
        applyAllSettings()
    }

    fun resetToFlat() {
        val size = _bands.value.size.coerceAtLeast(5)
        val flat = List(size) { 0 }
        _channelABands.value = flat
        _channelBBands.value = flat
        _subBoost.value = 0
        _masterGain.value = 0
        _midHighBoost.value = 0
        applyAllSettings()
    }

    private fun startAudioRouteMonitoring() {
        scope.launch {
            while (isActive) {
                updateBluetoothStatus()
                checkPermissions()
                delay(3000)
            }
        }
    }

    private fun updateBluetoothStatus() {
        var isBt = false
        var deviceName = "Speaker HP Bawaan"
        var audioType = "Speaker Internal"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                        isBt = true
                        val name = dev.productName.toString()
                        deviceName = if (name.isNotBlank()) name else "TWS Bluetooth Headset"
                        audioType = "Bluetooth TWS / A2DP"
                        break
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                        deviceName = "Headphone Kabel (Aux)"
                        audioType = "Wired Headset"
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
                isBt = true
                deviceName = "TWS Bluetooth Audio"
                audioType = "Bluetooth A2DP"
            }
        }

        _bluetoothStatus.value = BluetoothStatus(
            isConnected = isBt,
            deviceName = deviceName,
            isLowLatencyActive = _lowLatencyTwsEnabled.value,
            audioType = audioType
        )
    }

    private fun startAudioPlaybackMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                audioManager.registerAudioPlaybackCallback(object : AudioManager.AudioPlaybackCallback() {
                    override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                        val isMediaPlaying = configs.any { config ->
                            val usage = config.audioAttributes.usage
                            val contentType = config.audioAttributes.contentType
                            usage == AudioAttributes.USAGE_MEDIA ||
                            usage == AudioAttributes.USAGE_GAME ||
                            contentType == AudioAttributes.CONTENT_TYPE_MUSIC ||
                            contentType == AudioAttributes.CONTENT_TYPE_MOVIE
                        }

                        if (isMediaPlaying) {
                            lastRealAudioTimestamp = System.currentTimeMillis()
                            _isYouTubeAudioActive.value = true
                            if (hardwareVisualizer == null && _hasAudioPermission.value) {
                                initHardwareVisualizer(0)
                            }
                        }
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.w(TAG, "AudioPlaybackCallback registration not supported", e)
            }
        }
    }

    /**
     * Fallback and Decay engine:
     * When hardware visualizer is silent (or not granted), gently decays the meters to 0
     * so it never hangs or shows fake levels when YouTube is paused!
     */
    private fun startVuMeterEngine() {
        vuMeterFallbackJob?.cancel()
        vuMeterFallbackJob = scope.launch(Dispatchers.Default) {
            var synthStep = 0
            while (isActive) {
                synthStep++
                val isTonePlaying = toneGenerator.isPlaying
                val isRealAudioRecent = (System.currentTimeMillis() - lastRealAudioTimestamp) < 1500

                if (!_isEnabled.value) {
                    _vuLevelA.value = 0f
                    _vuLevelB.value = 0f
                    _peakLevelA.value = 0f
                    _peakLevelB.value = 0f
                    _spectrumLevels.value = listOf(0f, 0f, 0f, 0f, 0f)
                    _spectrumBands24.value = List(24) { 0f }
                    _spectrumPeaks24.value = List(24) { 0f }
                } else if (isTonePlaying) {
                    // Generate rich, responsive 24-band frequency spectrum for test tones
                    val curPeaks = _spectrumPeaks24.value.toMutableList()
                    val synth = FloatArray(24)
                    when (toneGenerator.currentMode) {
                        TestAudioMode.SUB_60HZ_PULSE -> {
                            val pulse = (Math.sin(synthStep * 0.35) * 0.25 + 0.75).toFloat()
                            for (i in 0 until 24) {
                                synth[i] = when (i) {
                                    2 -> (0.96f * pulse).coerceIn(0f, 1f) // 60Hz Sub peak!
                                    1, 3 -> (0.75f * pulse).coerceIn(0f, 1f)
                                    0, 4 -> (0.48f * pulse).coerceIn(0f, 1f)
                                    else -> (0.12f * Math.max(0f, 1f - (i - 2) * 0.08f)).coerceIn(0f, 1f)
                                }
                            }
                        }
                        TestAudioMode.HOREG_KICK_BEAT -> {
                            val beatPhase = (synthStep % 14) / 14.0
                            val kick = Math.exp(-beatPhase * 4.2).toFloat()
                            for (i in 0 until 24) {
                                synth[i] = when {
                                    i in 1..3 -> (kick * 0.98f).coerceIn(0.08f, 1f)
                                    i in 4..6 -> (kick * 0.65f).coerceIn(0.05f, 0.75f)
                                    i in 14..18 -> (kick * 0.42f).coerceIn(0.02f, 0.5f)
                                    else -> 0.06f
                                }
                            }
                        }
                        TestAudioMode.FULL_SWEEP_NOISE -> {
                            val sweepPos = (synthStep % 44) / 44.0 * 24.0
                            for (i in 0 until 24) {
                                val d = Math.abs(i - sweepPos)
                                synth[i] = Math.exp(-d * d / 4.5).toFloat().coerceIn(0.05f, 0.95f)
                            }
                        }
                    }
                    _spectrumBands24.value = synth.toList()
                    for (i in 0 until 24) {
                        val s = synth[i]
                        val p = curPeaks.getOrElse(i) { 0f }
                        curPeaks[i] = if (s > p) s else (p - 0.02f).coerceAtLeast(s)
                    }
                    _spectrumPeaks24.value = curPeaks
                    _spectrumLevels.value = listOf(synth[2], synth[5], synth[10], synth[16], synth[21])
                } else if (!isRealAudioRecent) {
                    // Smooth decay to zero when no audio is playing
                    _vuLevelA.value = (_vuLevelA.value - 0.05f).coerceAtLeast(0f)
                    _vuLevelB.value = (_vuLevelB.value - 0.05f).coerceAtLeast(0f)
                    _peakLevelA.value = (_peakLevelA.value - 0.03f).coerceAtLeast(_vuLevelA.value)
                    _peakLevelB.value = (_peakLevelB.value - 0.03f).coerceAtLeast(_vuLevelB.value)
                    val s = _spectrumLevels.value
                    _spectrumLevels.value = s.map { (it - 0.06f).coerceAtLeast(0f) }
                    val s24 = _spectrumBands24.value
                    val p24 = _spectrumPeaks24.value
                    _spectrumBands24.value = s24.map { (it - 0.05f).coerceAtLeast(0f) }
                    _spectrumPeaks24.value = p24.map { (it - 0.035f).coerceAtLeast(0f) }
                }
                delay(35)
            }
        }
    }

    fun release() {
        vuMeterFallbackJob?.cancel()
        toneGenerator.stop()
        try {
            hardwareVisualizer?.enabled = false
            hardwareVisualizer?.release()
        } catch (ignored: Exception) {}
        hardwareVisualizer = null

        activeSessions.keys.toList().forEach { detachSession(it) }
        activeSessions.clear()
    }
}
