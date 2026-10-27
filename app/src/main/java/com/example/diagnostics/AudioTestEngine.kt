package com.example.diagnostics

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

enum class AudioChannel {
    LEFT,
    RIGHT,
    STEREO_BOTH,
    EARPIECE
}

class AudioTestEngine(private val scope: CoroutineScope) {

    private var audioTrack: AudioTrack? = null
    private var toneJob: Job? = null
    private var sweepJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var micJob: Job? = null

    private val _isPlayingTone = MutableStateFlow(false)
    val isPlayingTone: StateFlow<Boolean> = _isPlayingTone.asStateFlow()

    private val _currentChannel = MutableStateFlow<AudioChannel?>(null)
    val currentChannel: StateFlow<AudioChannel?> = _currentChannel.asStateFlow()

    private val _isSweeping = MutableStateFlow(false)
    val isSweeping: StateFlow<Boolean> = _isSweeping.asStateFlow()

    private val _currentSweepFrequency = MutableStateFlow(0)
    val currentSweepFrequency: StateFlow<Int> = _currentSweepFrequency.asStateFlow()

    private val _isRecordingMic = MutableStateFlow(false)
    val isRecordingMic: StateFlow<Boolean> = _isRecordingMic.asStateFlow()

    private val _micDecibels = MutableStateFlow(0f)
    val micDecibels: StateFlow<Float> = _micDecibels.asStateFlow()

    private val _micPeakDecibels = MutableStateFlow(0f)
    val micPeakDecibels: StateFlow<Float> = _micPeakDecibels.asStateFlow()

    private val sampleRate = 44100

    fun playChannelTone(channel: AudioChannel, frequencyHz: Int = 440, durationMs: Long = 3000) {
        stopTone()
        toneJob = scope.launch(Dispatchers.Default) {
            _isPlayingTone.value = true
            _currentChannel.value = channel

            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val usage = if (channel == AudioChannel.EARPIECE) {
                    AudioAttributes.USAGE_VOICE_COMMUNICATION
                } else {
                    AudioAttributes.USAGE_MEDIA
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(if (channel == AudioChannel.EARPIECE) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC)
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
                    .build()

                audioTrack = track
                track.play()

                val numSamples = sampleRate / 10 // 100ms chunk
                val shortBuffer = ShortArray(numSamples * 2) // stereo = 2 samples per frame
                var sampleIndex = 0L

                val startTime = System.currentTimeMillis()
                while (isActive && (durationMs <= 0 || System.currentTimeMillis() - startTime < durationMs)) {
                    for (i in 0 until numSamples) {
                        val angle = 2.0 * Math.PI * sampleIndex * frequencyHz / sampleRate
                        val amplitude = if (channel == AudioChannel.EARPIECE) 0.5f else 0.75f
                        val sampleVal = (sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
                        sampleIndex++

                        when (channel) {
                            AudioChannel.LEFT -> {
                                shortBuffer[2 * i] = sampleVal       // Left
                                shortBuffer[2 * i + 1] = 0           // Right mute
                            }
                            AudioChannel.RIGHT -> {
                                shortBuffer[2 * i] = 0               // Left mute
                                shortBuffer[2 * i + 1] = sampleVal   // Right
                            }
                            AudioChannel.STEREO_BOTH, AudioChannel.EARPIECE -> {
                                shortBuffer[2 * i] = sampleVal       // Left
                                shortBuffer[2 * i + 1] = sampleVal   // Right
                            }
                        }
                    }
                    track.write(shortBuffer, 0, shortBuffer.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopTrackQuietly()
                _isPlayingTone.value = false
                _currentChannel.value = null
            }
        }
    }

    fun playFrequencySweep(startHz: Int = 200, endHz: Int = 4000, durationMs: Long = 4000) {
        stopTone()
        sweepJob = scope.launch(Dispatchers.Default) {
            _isSweeping.value = true
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val track = AudioTrack.Builder()
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
                    .build()

                audioTrack = track
                track.play()

                val stepIntervalMs = 50L
                val steps = (durationMs / stepIntervalMs).toInt().coerceAtLeast(1)
                val freqStep = (endHz - startHz).toFloat() / steps

                val chunkSamples = (sampleRate * (stepIntervalMs / 1000f)).toInt()
                val shortBuffer = ShortArray(chunkSamples * 2)
                var currentFreq = startHz.toFloat()
                var phase = 0.0

                for (step in 0 until steps) {
                    if (!isActive) break
                    _currentSweepFrequency.value = currentFreq.toInt()

                    for (i in 0 until chunkSamples) {
                        phase += 2.0 * Math.PI * currentFreq / sampleRate
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                        val sampleVal = (sin(phase) * Short.MAX_VALUE * 0.75).toInt().toShort()
                        shortBuffer[2 * i] = sampleVal
                        shortBuffer[2 * i + 1] = sampleVal
                    }
                    track.write(shortBuffer, 0, shortBuffer.size)
                    currentFreq += freqStep
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopTrackQuietly()
                _isSweeping.value = false
                _currentSweepFrequency.value = 0
            }
        }
    }

    fun stopTone() {
        toneJob?.cancel()
        toneJob = null
        sweepJob?.cancel()
        sweepJob = null
        stopTrackQuietly()
        _isPlayingTone.value = false
        _currentChannel.value = null
        _isSweeping.value = false
        _currentSweepFrequency.value = 0
    }

    private fun stopTrackQuietly() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    @SuppressLint("MissingPermission")
    fun startMicMonitor() {
        stopMicMonitor()
        _micPeakDecibels.value = 0f
        micJob = scope.launch(Dispatchers.IO) {
            val recordBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 10)

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    recordBufferSize
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    return@launch
                }

                audioRecord = record
                record.startRecording()
                _isRecordingMic.value = true

                val buffer = ShortArray(recordBufferSize / 2)

                while (isActive) {
                    val readCount = record.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var sumSquared = 0.0
                        for (i in 0 until readCount) {
                            sumSquared += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sumSquared / readCount)
                        val db = if (rms > 1.0) {
                            (20 * log10(rms / 32767.0) + 90.0).toFloat().coerceIn(0f, 100f)
                        } else {
                            0f
                        }

                        _micDecibels.value = db
                        if (db > _micPeakDecibels.value) {
                            _micPeakDecibels.value = db
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopMicQuietly()
                _isRecordingMic.value = false
                _micDecibels.value = 0f
            }
        }
    }

    fun stopMicMonitor() {
        micJob?.cancel()
        micJob = null
        stopMicQuietly()
        _isRecordingMic.value = false
    }

    private fun stopMicQuietly() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        audioRecord = null
    }

    suspend fun runAutomatedSpeakerMicProbe(): Boolean = withContext(Dispatchers.Default) {
        // Plays a short tone and checks for peak response
        try {
            playChannelTone(AudioChannel.STEREO_BOTH, 1000, 1200)
            delay(1300)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun releaseAll() {
        stopTone()
        stopMicMonitor()
    }
}
