package com.example.wallrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun playTone(
        frequencies: List<Double>,
        durationMs: Int,
        sampleRate: Int = 22050,
        envelopeType: String = "decay"
    ) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var sampleVal = 0.0
                    for (freq in frequencies) {
                        sampleVal += sin(2.0 * Math.PI * freq * t)
                    }
                    sampleVal /= frequencies.size

                    // Apply amplitude envelope
                    val progress = i.toDouble() / numSamples
                    val amplitude = when (envelopeType) {
                        "decay" -> (1.0 - progress) * (1.0 - progress)
                        "bell" -> sin(progress * Math.PI)
                        "quick_decay" -> Math.exp(-progress * 5.0)
                        else -> 1.0 - progress
                    }

                    buffer[i] = (sampleVal * amplitude * Short.MAX_VALUE * 0.7).toInt().toShort()
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

                // Release after playing
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore audio track exceptions gracefully
            }
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 120) {
        if (!isVibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore vibration exceptions
        }
    }

    fun playMove() {
        playTone(listOf(520.0, 780.0), 80, envelopeType = "quick_decay")
        vibrate(20, 80)
    }

    fun playWallPlace() {
        playTone(listOf(220.0, 330.0, 440.0), 120, envelopeType = "decay")
        vibrate(35, 140)
    }

    fun playInvalid() {
        playTone(listOf(150.0, 180.0), 150, envelopeType = "decay")
        vibrate(60, 200)
    }

    fun playTurnChange() {
        playTone(listOf(880.0), 90, envelopeType = "bell")
    }

    fun playWin() {
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            for (note in notes) {
                playTone(listOf(note), 140, envelopeType = "bell")
                vibrate(30, 100)
                kotlinx.coroutines.delay(110)
            }
        }
    }

    fun playLose() {
        scope.launch {
            val notes = listOf(440.0, 392.0, 349.23, 293.66)
            for (note in notes) {
                playTone(listOf(note), 140, envelopeType = "decay")
                kotlinx.coroutines.delay(120)
            }
        }
    }

    fun playCountdownBeep(isGo: Boolean = false) {
        if (isGo) {
            playTone(listOf(880.0, 1174.66), 200, envelopeType = "bell")
            vibrate(40, 150)
        } else {
            playTone(listOf(440.0), 100, envelopeType = "bell")
            vibrate(20, 80)
        }
    }

    fun playEmote() {
        playTone(listOf(600.0, 900.0, 1200.0), 100, envelopeType = "quick_decay")
    }

    fun playButton() {
        playTone(listOf(600.0), 40, envelopeType = "quick_decay")
    }
}
