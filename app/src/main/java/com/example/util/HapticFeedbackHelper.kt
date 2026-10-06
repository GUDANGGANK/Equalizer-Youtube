package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.abs

object HapticFeedbackHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Tactile response specifically tuned for the 60Hz Horeg Sub-Bass slider.
     * Higher levels yield deeper and more intense physical tactile pulses.
     */
    fun performSubBassHaptic(
        context: Context,
        currentValue: Int, // 0..1000
        previousValue: Int,
        view: View? = null
    ) {
        if (currentValue == previousValue) return

        val stepInterval = 40 // Every 4% on slider
        val currentStep = currentValue / stepInterval
        val prevStep = previousValue / stepInterval

        // Check if stepped or hit milestone
        val isStepCrossed = currentStep != prevStep
        val isBoundary = (currentValue == 0 || currentValue == 1000) && (previousValue != currentValue)
        val isGlerrMilestone = (currentValue >= 900 && previousValue < 900) || (currentValue >= 500 && previousValue < 500)

        if (!isStepCrossed && !isBoundary && !isGlerrMilestone) return

        val vibrator = getVibrator(context)

        try {
            if (isBoundary || isGlerrMilestone) {
                // Intense tactile rumble for Sub-Bass Glerr thresholds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    vibrator?.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } else if (isStepCrossed) {
                // Tactile tick scaled with bass intensity (stronger rumble as bass level increases)
                val amplitude = (50 + (currentValue / 1000f) * 180f).toInt().coerceIn(1, 255)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(12, amplitude)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(12)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        } catch (ignored: Exception) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Tactile response for Master Gain Pre-Amp slider adjustments.
     */
    fun performGainHaptic(
        context: Context,
        currentValue: Int, // 0..1000
        previousValue: Int,
        view: View? = null
    ) {
        if (currentValue == previousValue) return

        val stepInterval = 50 // Every 5%
        val currentStep = currentValue / stepInterval
        val prevStep = previousValue / stepInterval

        val isStepCrossed = currentStep != prevStep
        val isBoundary = (currentValue == 0 || currentValue == 1000) && (previousValue != currentValue)

        if (!isStepCrossed && !isBoundary) return

        val vibrator = getVibrator(context)

        try {
            if (isBoundary) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    vibrator?.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(25, 200)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } else if (isStepCrossed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    vibrator?.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(8, 90)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(8)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        } catch (ignored: Exception) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Tactile response for EQ vertical faders (crossing 0 dB detent or steps).
     */
    fun performFaderHaptic(
        context: Context,
        newDb: Int,
        oldDb: Int,
        view: View? = null
    ) {
        if (newDb == oldDb) return

        val vibrator = getVibrator(context)
        try {
            if (newDb == 0) {
                // Firm tactile click at center detent 0 dB
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    vibrator?.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(20)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } else if (abs(newDb) == 15) {
                // Boundary limit
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    vibrator?.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(25, 220)
                    vibrator?.vibrate(effect)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } else {
                // Standard fader step tick
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    vibrator?.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(6, 75)
                    vibrator?.vibrate(effect)
                }
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        } catch (ignored: Exception) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
