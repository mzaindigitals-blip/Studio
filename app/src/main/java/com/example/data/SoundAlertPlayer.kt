package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Plays a crisp dual-tone trading terminal alert chime using PCM AudioTrack synthesis
 * (zero external audio files or paid APIs required), with ToneGenerator fallback.
 */
class SoundAlertPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun playSignalAlert() {
        scope.launch {
            try {
                playSynthesizedChime()
            } catch (e: Exception) {
                Log.w("SoundAlertPlayer", "AudioTrack failed, falling back to ToneGenerator", e)
                playFallbackTone()
            }
        }
    }

    private fun playSynthesizedChime() {
        val sampleRate = 44100
        val totalDurationSec = 0.42
        val totalSamples = (sampleRate * totalDurationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Two-note ascending terminal chime: A5 (880 Hz) -> E6 (1318.5 Hz)
        val note1End = (sampleRate * 0.14).toInt()
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val (freq, localT) = if (i < note1End) {
                880.0 to t
            } else {
                1318.51 to (i - note1End).toDouble() / sampleRate
            }
            val attack = (localT / 0.012).coerceAtMost(1.0)
            val decay = exp(-localT * 9.5)
            val wave = sin(2.0 * PI * freq * localT) + 0.25 * sin(4.0 * PI * freq * localT)
            val sample = (wave * attack * decay * 0.45 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSizeBytes = maxOf(minBuffer, buffer.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
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
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        Thread.sleep(460)
        audioTrack.stop()
        audioTrack.release()
    }

    private fun playFallbackTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            Thread.sleep(300)
            toneGen.release()
        } catch (e: Exception) {
            Log.w("SoundAlertPlayer", "Fallback ToneGenerator failed", e)
        }
    }
}
