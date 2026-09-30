package com.twocall.chat.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class VoiceRecorder(private val context: Context) {

    private val tag = "VoiceRecorder"
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude = _currentAmplitude.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds = _durationSeconds.asStateFlow()

    private var amplitudeJob: Job? = null

    fun startRecording(): File? {
        val outputDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
        val file = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
                _isRecording.value = true
                _durationSeconds.value = 0
                startAmplitudePolling()
                Log.i(tag, "Voice recording started to ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start recording: ${e.message}")
                release()
                currentOutputFile = null
                return null
            }
        }

        return file
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = CoroutineScope(Dispatchers.Default).launch {
            var seconds = 0
            var ticks = 0
            while (_isRecording.value) {
                delay(100)
                try {
                    val amp = mediaRecorder?.maxAmplitude ?: 0
                    _currentAmplitude.value = amp
                } catch (e: Exception) {
                    // Ignore transient recorder exceptions
                }
                ticks++
                if (ticks % 10 == 0) {
                    seconds++
                    _durationSeconds.value = seconds
                }
            }
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        amplitudeJob?.cancel()
        _isRecording.value = false

        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            Log.i(tag, "Voice recording stopped")
            currentOutputFile
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recorder: ${e.message}")
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
        amplitudeJob?.cancel()
        _isRecording.value = false
        try {
            mediaRecorder?.stop()
        } catch (ignored: Exception) {}
        try {
            mediaRecorder?.release()
        } catch (ignored: Exception) {}
        mediaRecorder = null

        currentOutputFile?.delete()
        currentOutputFile = null
    }
}
