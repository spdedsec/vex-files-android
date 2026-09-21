package com.vex.files.core

import android.content.Context
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    fun bytes(value: Long): String {
        if (value < 1024) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        val exp = (log10(value.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.size)
        return String.format(Locale.US, "%.1f %s", value / 1024.0.pow(exp.toDouble()), units[exp - 1])
    }

    fun date(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(value))

    fun storage(path: String): Triple<Long, Long, Int> {
        val stat = StatFs(path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val percent = if (total == 0L) 0 else (((total - free) * 100.0) / total).toInt()
        return Triple(total, free, percent)
    }

    fun mime(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif" -> "image/*"
            "mp4", "mkv", "webm", "avi", "mov" -> "video/*"
            "mp3", "m4a", "wav", "ogg", "flac" -> "audio/*"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "json" -> "application/json"
            "txt", "md", "log", "csv", "xml", "kt", "java", "py", "js", "ts", "css", "html" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
