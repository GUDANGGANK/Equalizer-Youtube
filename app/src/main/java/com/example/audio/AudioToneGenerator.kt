package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class TestAudioMode {
    SUB_60HZ_PULSE,
    HOREG_KICK_BEAT,
    FULL_SWEEP_NOISE
}

class AudioToneGenerator {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var isPlaying: Boolean = false
        private set

    var currentMode: TestAudioMode = TestAudioMode.SUB_60HZ_PULSE
        private set

    var onAmplitudeUpdate: ((left: Float, right: Float) -> Unit)? = null

    fun start(mode: TestAudioMode = TestAudioMode.SUB_60HZ_PULSE, sessionId: Int = 0) {
        stop()
        currentMode = mode
        isPlaying = true

        playbackJob = scope.launch {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (sessionId > 0) {
                trackBuilder.setSessionId(sessionId)
            }

            val track = trackBuilder.build()

            audioTrack = track
            track.play()

            val pcmBuffer = ShortArray(bufferSize / 2)
            var phase60 = 0.0
            var sampleIndex = 0L

            try {
                while (isActive && isPlaying) {
                    when (currentMode) {
                        TestAudioMode.SUB_60HZ_PULSE -> {
                            val pulseRate = 1.5 // 1.5 Hz pulse envelope
                            for (i in 0 until pcmBuffer.size step 2) {
                                val t = sampleIndex.toDouble() / sampleRate
                                val envelope = (0.5 * (1.0 + sin(2 * PI * pulseRate * t))).coerceIn(0.1, 1.0)
                                // Pure 60Hz Sub-bass sine wave
                                val sample = (sin(2 * PI * 60.0 * t) * envelope * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
                                pcmBuffer[i] = sample     // Left Channel (A)
                                pcmBuffer[i + 1] = sample // Right Channel (B)
                                sampleIndex++
                            }
                            onAmplitudeUpdate?.invoke(0.85f, 0.82f)
                        }
                        TestAudioMode.HOREG_KICK_BEAT -> {
                            // Synthesize rhythmic kick (60-110Hz punch) + snappy hi-hat (8-12kHz)
                            val beatDuration = (sampleRate * 0.45).toInt() // ~133 BPM
                            for (i in 0 until pcmBuffer.size step 2) {
                                val beatPos = (sampleIndex % beatDuration).toInt()
                                val kickEnvelope = (1.0 - (beatPos.toDouble() / (sampleRate * 0.28))).coerceAtLeast(0.0)
                                val kickFreq = 120.0 * kickEnvelope + 55.0 // pitch drop into 55-60Hz sub-bass
                                val kickSample = sin(2 * PI * kickFreq * (beatPos.toDouble() / sampleRate)) * kickEnvelope

                                // Hi-hat / shaker on off-beat (mid-high test)
                                val hatPos = ((sampleIndex + beatDuration / 2) % beatDuration).toInt()
                                val hatEnvelope = (1.0 - (hatPos.toDouble() / (sampleRate * 0.06))).coerceAtLeast(0.0)
                                val noise = (Math.random() * 2.0 - 1.0) * hatEnvelope * 0.35

                                val mixed = ((kickSample * 0.85 + noise) * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
                                pcmBuffer[i] = mixed
                                pcmBuffer[i + 1] = mixed
                                sampleIndex++
                            }
                            onAmplitudeUpdate?.invoke(0.92f, 0.90f)
                        }
                        TestAudioMode.FULL_SWEEP_NOISE -> {
                            for (i in 0 until pcmBuffer.size step 2) {
                                val t = (sampleIndex % (sampleRate * 3)).toDouble() / (sampleRate * 3)
                                // Frequency sweep from 40Hz to 16000Hz logarithmically
                                val freq = 40.0 * Math.pow(400.0, t)
                                val s = (sin(2 * PI * freq * (sampleIndex.toDouble() / sampleRate)) * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
                                pcmBuffer[i] = s
                                pcmBuffer[i + 1] = s
                                sampleIndex++
                            }
                            onAmplitudeUpdate?.invoke(0.78f, 0.76f)
                        }
                    }

                    track.write(pcmBuffer, 0, pcmBuffer.size)
                }
            } catch (e: Exception) {
                // Ignore audio track write exceptions during teardown
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (ignored: Exception) {}
            }
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
        onAmplitudeUpdate?.invoke(0f, 0f)
    }

    /**
     * Plays a focused stereo acoustic chime in either the Left (L) or Right (R) channel
     * to allow user to calibrate and verify TWS earbud pairing and balance.
     */
    fun playChannelChime(isLeft: Boolean, sessionId: Int = 0) {
        stop()
        isPlaying = true
        playbackJob = scope.launch {
            val sampleRate = 44100
            val numSamples = (sampleRate * 0.45).toInt()
            val pcmBuffer = ShortArray(numSamples * 2)

            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(pcmBuffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)

            if (sessionId > 0) {
                trackBuilder.setSessionId(sessionId)
            }

            val track = trackBuilder.build()

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = Math.sin(Math.PI * i / numSamples).toFloat()
                val sample = (Math.sin(2 * Math.PI * 880.0 * t) * env * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
                val idx = i * 2
                if (isLeft) {
                    pcmBuffer[idx] = sample
                    pcmBuffer[idx + 1] = 0
                } else {
                    pcmBuffer[idx] = 0
                    pcmBuffer[idx + 1] = sample
                }
            }

            track.write(pcmBuffer, 0, pcmBuffer.size)
            track.play()
            if (isLeft) onAmplitudeUpdate?.invoke(0.85f, 0f) else onAmplitudeUpdate?.invoke(0f, 0.85f)
            delay(500)
            try {
                track.stop()
                track.release()
            } catch (ignored: Exception) {}
            isPlaying = false
            onAmplitudeUpdate?.invoke(0f, 0f)
        }
    }
}
