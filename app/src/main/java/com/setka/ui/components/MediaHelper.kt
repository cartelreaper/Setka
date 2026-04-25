package com.setka.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object MediaHelper {

    /**
     * Копирует файл из Uri во внутреннее хранилище приложения.
     * Возвращает File или null при ошибке.
     */
    fun copyToInternal(context: Context, uri: Uri, subDir: String): File? {
        return try {
            val name = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
            val dir = File(context.filesDir, subDir).also { it.mkdirs() }
            val dest = File(dir, "${System.currentTimeMillis()}_$name")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0) dest else null
        } catch (e: Exception) {
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getString(idx) else null
        }
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getLong(idx) else 0L
        } ?: 0L
    }

    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024          -> "$bytes Б"
        bytes < 1024 * 1024   -> "${bytes / 1024} КБ"
        else                  -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} МБ"
    }

    fun formatDuration(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
}
