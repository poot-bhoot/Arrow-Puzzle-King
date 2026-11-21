package com.arrowpuzzle.king.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundAndHapticEngine(private val context: Context) {

    var soundEnabled: Boolean = true
    var hapticEnabled: Boolean = true

    private val sampleRate = 44100

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playTap() {
        triggerHaptic(18L, 80)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.055) { t ->
            val env = exp(-t * 65.0)
            val freq = 620.0 + 380.0 * exp(-t * 80.0)
            sin(2.0 * PI * freq * t) * env * 0.45
        }
    }

    fun playWhooshEscape() {
        triggerHaptic(32L, 140)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.22) { t ->
            // Smooth rising harmonic chime + airy slide
            val env = sin(PI * (t / 0.22)).coerceIn(0.0, 1.0) * exp(-t * 5.5)
            val freq1 = 440.0 + (t / 0.22) * 520.0
            val freq2 = freq1 * 1.5
            (sin(2.0 * PI * freq1 * t) * 0.55 + sin(2.0 * PI * freq2 * t) * 0.25) * env * 0.55
        }
    }

    fun playBlockedCollision() {
        triggerHaptic(75L, 230)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.20) { t ->
            // Low double-thud warning buzz
            val pulse = if (t < 0.08) 1.0 else if (t > 0.10) 0.85 else 0.15
            val env = exp(-t * 14.0) * pulse
            val freq = 155.0 - t * 140.0
            val wave = sin(2.0 * PI * freq * t) + 0.35 * sin(2.0 * PI * freq * 2.0 * t)
            wave * env * 0.65
        }
    }

    fun playHintChime() {
        triggerHaptic(25L, 110)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.32) { t ->
            val note = when {
                t < 0.10 -> 587.33 // D5
                t < 0.20 -> 783.99 // G5
                else -> 1046.50    // C6
            }
            val localT = t % 0.10
            val env = exp(-localT * 18.0)
            sin(2.0 * PI * note * t) * env * 0.45
        }
    }

    fun playLevelCompleteFanfare() {
        triggerHaptic(90L, 255)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.65) { t ->
            val note = when {
                t < 0.12 -> 523.25 // C5
                t < 0.24 -> 659.25 // E5
                t < 0.36 -> 783.99 // G5
                else -> 1046.50    // C6
            }
            val localT = if (t < 0.36) (t % 0.12) else (t - 0.36)
            val decay = if (t < 0.36) 14.0 else 5.0
            val env = exp(-localT * decay)
            val chord = sin(2.0 * PI * note * t) * 0.6 + sin(2.0 * PI * (note * 0.5) * t) * 0.3
            chord * env * 0.55
        }
    }

    fun playGameOver() {
        triggerHaptic(120L, 255)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.55) { t ->
            val note = when {
                t < 0.18 -> 392.00 // G4
                t < 0.36 -> 349.23 // F4
                else -> 293.66     // D4
            }
            val localT = t % 0.18
            val env = exp(-localT * 9.0)
            sin(2.0 * PI * note * t) * env * 0.50
        }
    }

    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        if (!hapticEnabled) return
        try {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun playGeneratedPcm(durationSec: Double, generator: (Double) -> Double) {
        thread(start = true) {
            try {
                val numSamples = (durationSec * sampleRate).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sample = generator(t).coerceIn(-1.0, 1.0)
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep((durationSec * 1000).toLong() + 40L)
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }
}
