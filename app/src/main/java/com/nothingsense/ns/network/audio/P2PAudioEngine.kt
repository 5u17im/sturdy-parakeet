package com.nothingsense.ns.network.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "P2PAudioEngine"
private const val SAMPLE_RATE = 16000
private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

@Singleton
class P2PAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var isRecording = false

    @Volatile
    private var isPlaying = false

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onAudioFrameCaptured: (ByteArray) -> Unit) {
        if (isRecording) return
        isRecording = true

        recordingJob = scope.launch {
            var audioRecord: AudioRecord? = null
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
                val bufferSize = Math.max(minBufferSize, 2048)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize")
                    isRecording = false
                    return@launch
                }

                audioRecord.startRecording()
                Log.d(TAG, "Started AudioRecord capture...")

                val buffer = ByteArray(1024)
                while (isActive && isRecording) {
                    val readBytes = audioRecord.read(buffer, 0, buffer.size)
                    if (readBytes > 0) {
                        val frame = buffer.copyOf(readBytes)
                        onAudioFrameCaptured(frame)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio recording stream", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing AudioRecord", e)
                }
                isRecording = false
                Log.d(TAG, "Stopped AudioRecord capture.")
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
    }

    fun playAudioFrame(frame: ByteArray) {
        try {
            val track = audioTrack ?: return
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            track.write(frame, 0, frame.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio frame", e)
        }
    }

    fun release() {
        stopRecording()
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
    }
}
