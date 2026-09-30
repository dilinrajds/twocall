package com.twocall.chat.audio

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class VoicePlayer {

    private val tag = "VoicePlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath = _currentPlayingPath.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction = _progressFraction.asStateFlow()

    fun play(filePath: String) {
        if (_isPlaying.value && _currentPlayingPath.value == filePath) {
            pause()
            return
        }

        stop()

        val file = File(filePath)
        if (!file.exists()) {
            Log.e(tag, "Audio file not found: $filePath")
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                _isPlaying.value = true
                _currentPlayingPath.value = filePath

                setOnCompletionListener {
                    stop()
                }

                startProgressTracking()
            } catch (e: Exception) {
                Log.e(tag, "Failed to play audio: ${e.message}")
                stop()
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Default).launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying && player.duration > 0) {
                            _progressFraction.value = player.currentPosition.toFloat() / player.duration
                        }
                    } catch (ignored: Exception) {}
                }
                delay(100)
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e(tag, "Pause error: ${e.message}")
        }
    }

    fun stop() {
        progressJob?.cancel()
        _isPlaying.value = false
        _progressFraction.value = 0f
        _currentPlayingPath.value = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
