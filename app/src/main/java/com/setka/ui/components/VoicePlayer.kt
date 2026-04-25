package com.setka.ui.components

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentMessageId: String? = null,
    val progress: Float = 0f,
    val durationMs: Long = 0L
)

object VoicePlayer {

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun play(messageId: String, filePath: String, scope: CoroutineScope) {
        if (_state.value.currentMessageId == messageId && _state.value.isPlaying) {
            pause()
            return
        }
        stop()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(filePath)
            mp.prepare()
            mp.start()
            player = mp

            val dur: Long = mp.duration.toLong()
            _state.value = PlaybackState(
                isPlaying = true,
                currentMessageId = messageId,
                durationMs = dur
            )

            mp.setOnCompletionListener {
                _state.value = PlaybackState()
                progressJob?.cancel()
            }

            progressJob = playerScope.launch {
                while (player?.isPlaying == true) {
                    val pos: Long = player?.currentPosition?.toLong() ?: 0L
                    val total: Long = player?.duration?.toLong() ?: 1L
                    val prog: Float = if (total > 0) pos.toFloat() / total.toFloat() else 0f
                    _state.value = _state.value.copy(progress = prog)
                    delay(100)
                }
            }
        } catch (e: Exception) {
            _state.value = PlaybackState()
        }
    }

    fun pause() {
        player?.pause()
        progressJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun stop() {
        progressJob?.cancel()
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        _state.value = PlaybackState()
    }
}
