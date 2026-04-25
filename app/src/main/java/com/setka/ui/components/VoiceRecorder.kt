package com.setka.ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0L

    fun start(): File? {
        return try {
            val dir = File(context.filesDir, "voices").also { it.mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.ogg")
            outputFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(32000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            startTime = System.currentTimeMillis()
            file
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }

    fun stop(): Pair<File, Long>? {
        val file = outputFile ?: return null
        val duration = System.currentTimeMillis() - startTime
        return try {
            recorder?.apply { stop(); release() }
            recorder = null
            if (file.exists() && file.length() > 0) Pair(file, duration) else null
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }

    fun cancel() {
        try {
            recorder?.apply { stop(); release() }
        } catch (_: Exception) {}
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    fun isRecording() = recorder != null
}
