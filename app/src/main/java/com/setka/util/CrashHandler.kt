package com.setka.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Перехватывает необработанные исключения, пишет лог на диск.
 * При следующем запуске можно отправить разработчику.
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "SetkaError"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val log = buildString {
                appendLine("=== Setka Crash Report ===")
                appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.message}")
                appendLine("Stack trace:")
                appendLine(throwable.stackTraceToString())
            }

            Log.e(TAG, log)

            // Сохраняем на диск
            appContext?.let { ctx ->
                val dir  = File(ctx.filesDir, "crashes").also { it.mkdirs() }
                val file = File(dir, "crash_${System.currentTimeMillis()}.txt")
                file.writeText(log)
                // Удаляем старые логи (оставляем только последние 5)
                dir.listFiles()
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(5)
                    ?.forEach { it.delete() }
            }
        } catch (_: Exception) {}

        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun getLastCrashLog(context: Context): String? {
        val dir = File(context.filesDir, "crashes")
        return dir.listFiles()
            ?.maxByOrNull { it.lastModified() }
            ?.readText()
    }
}
