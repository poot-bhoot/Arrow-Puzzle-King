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
import kotlin.random.Random

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

    /** Tactile wooden/glass micro-click for UI & Store interactions */
    fun playTap() {
        vibrateOneShot(22L, 120)
        if (!soundEnabled) return
        playGeneratedPcm(durationSec = 0.065) { t ->
            val env = exp(-t * 68.0)
            val fundamental = sin(2.0 * PI * 740.0 * t)
            val overtone = sin(2.0 * PI * 1480.0 * t) * 0.35
            val bodyThump = sin(2.0 * PI * 210.0 * t) * exp(-t * 110.0) * 0.45
            (fundamental * 0.55 + overtone + bodyThump) * env * 0.50
        }
    }

    /** Crisp silk-glide whoosh + harmonic marimba chime when arrow uncoils along its path */
    fun playWhooshEscape(comboCount: Int = 1) {
        // Double-pulse kinetic vibration that mimics an arrow zipping out
        vibrateWaveform(longArrayOf(0, 22, 28, 35), intArrayOf(0, 140, 0, 210))
        if (!soundEnabled) return

        val pitchBoost = (comboCount.coerceIn(1, 8) - 1) * 38.0
        val rng = Random(comboCount * 31)

        playGeneratedPcm(durationSec = 0.28) { t ->
            val norm = (t / 0.28).coerceIn(0.0, 1.0)
            // Smooth airy whoosh (filtered noise + rising sine)
            val airEnv = sin(PI * norm) * exp(-t * 4.5)
            val airNoise = (rng.nextDouble() * 2.0 - 1.0) * 0.14 * airEnv

            // Warm marimba-bell glide
            val chimeEnv = exp(-t * 11.0)
            val baseFreq = 480.0 + pitchBoost + norm * 420.0
            val chime = sin(2.0 * PI * baseFreq * t) * 0.55 +
                sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.25 +
                sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.12

            (chime * chimeEnv + airNoise) * 0.62
        }
    }

    /** Heavy rubber-bumper double thud when arrow head hits a blocker */
    fun playBlockedCollision() {
        // Strong double-impact vibration
        vibrateWaveform(longArrayOf(0, 55, 45, 85), intArrayOf(0, 230, 0, 255))
        if (!soundEnabled) return

        playGeneratedPcm(durationSec = 0.24) { t ->
            val hit1 = exp(-t * 28.0)
            val hit2 = if (t >= 0.085) exp(-(t - 0.085) * 24.0) * 0.9 else 0.0
            val env = hit1 + hit2
            val freq = 145.0 - t * 120.0
            val sub = sin(2.0 * PI * freq * t)
            val buzz = sin(2.0 * PI * (freq * 2.5) * t) * 0.35
            (sub + buzz) * env * 0.65
        }
    }

    /** Magical 3-note crystal arpeggio for Smart Hint */
    fun playHintChime() {
        vibrateWaveform(longArrayOf(0, 20, 40, 25), intArrayOf(0, 110, 0, 160))
        if (!soundEnabled) return

        playGeneratedPcm(durationSec = 0.38) { t ->
            val note = when {
                t < 0.11 -> 659.25 // E5
                t < 0.22 -> 830.61 // G#5
                else -> 1174.66    // D6
            }
            val localT = when {
                t < 0.11 -> t
                t < 0.22 -> t - 0.11
                else -> t - 0.22
            }
            val env = exp(-localT * 15.0)
            val shimmer = sin(2.0 * PI * note * t) * 0.65 + sin(2.0 * PI * (note * 2.0) * t) * 0.22
            shimmer * env * 0.50
        }
    }

    /** Metallic coin clink + sparkle for Store purchases */
    fun playCoinPurchase() {
        vibrateWaveform(longArrayOf(0, 25, 35, 45), intArrayOf(0, 160, 0, 240))
        if (!soundEnabled) return

        playGeneratedPcm(durationSec = 0.36) { t ->
            val freq = if (t < 0.09) 987.77 else 1318.51 // B5 -> E6
            val localT = if (t < 0.09) t else (t - 0.09)
            val env = exp(-localT * 13.0)
            val metallic = sin(2.0 * PI * freq * t) * 0.6 + sin(2.0 * PI * (freq * 2.41) * t) * 0.25
            metallic * env * 0.52
        }
    }

    /** Grand orchestral-bell victory fanfare */
    fun playLevelCompleteFanfare() {
        vibrateWaveform(longArrayOf(0, 45, 50, 45, 50, 110), intArrayOf(0, 180, 0, 210, 0, 255))
        if (!soundEnabled) return

        playGeneratedPcm(durationSec = 0.80) { t ->
            val note = when {
                t < 0.13 -> 523.25 // C5
                t < 0.26 -> 659.25 // E5
                t < 0.39 -> 783.99 // G5
                else -> 1046.50    // C6
            }
            val localT = if (t < 0.39) (t % 0.13) else (t - 0.39)
            val decay = if (t < 0.39) 13.0 else 4.2
            val env = exp(-localT * decay)
            val voice1 = sin(2.0 * PI * note * t) * 0.55
            val voice2 = sin(2.0 * PI * (note * 0.5) * t) * 0.30
            val voice3 = sin(2.0 * PI * (note * 1.25) * t) * 0.18
            (voice1 + voice2 + voice3) * env * 0.58
        }
    }

    /** Soft descending tone for Out of Lives */
    fun playGameOver() {
        vibrateWaveform(longArrayOf(0, 90, 60, 140), intArrayOf(0, 220, 0, 255))
        if (!soundEnabled) return

        playGeneratedPcm(durationSec = 0.60) { t ->
            val note = when {
                t < 0.20 -> 392.00 // G4
                t < 0.40 -> 349.23 // F4
                else -> 293.66     // D4
            }
            val localT = t % 0.20
            val env = exp(-localT * 8.5)
            sin(2.0 * PI * note * t) * env * 0.50
        }
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
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

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        if (!hapticEnabled) return
        try {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vib.hasAmplitudeControl()) {
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vib.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
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
                Thread.sleep((durationSec * 1000).toLong() + 45L)
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }
}
