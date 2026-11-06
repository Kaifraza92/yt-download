package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "—"
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatEta(etaSeconds: Long): String {
        if (etaSeconds < 0L) return "—"
        if (etaSeconds == 0L) return "0s"
        val mins = etaSeconds / 60
        val secs = etaSeconds % 60
        return if (mins > 0) {
            "${mins}m ${secs}s"
        } else {
            "${secs}s"
        }
    }

    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "LIVE / STREAM"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(epochMillis))
    }
}
