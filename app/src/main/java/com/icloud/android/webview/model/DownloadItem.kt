package com.icloud.android.webview.model

import java.io.Serializable
import java.util.Date

data class DownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    var downloadedSize: Long,
    var totalSize: Long,
    var progress: Int,
    var status: Int,
    val timeStarted: Date,
    var fileUri: String? = null
) : Serializable {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_COMPLETED = 3
        const val STATUS_CANCELLED = 4
        const val STATUS_FAILED = 5
        
        fun getStatusText(status: Int): String {
            return when (status) {
                STATUS_PENDING -> "Pending"
                STATUS_DOWNLOADING -> "Downloading"
                STATUS_PAUSED -> "Paused"
                STATUS_COMPLETED -> "Completed"
                STATUS_CANCELLED -> "Cancelled"
                STATUS_FAILED -> "Failed"
                else -> "Unknown Status"
            }
        }
    }
    
    fun getFormattedSize(): String {
        return if (totalSize <= 0) {
            "Unknown Size"
        } else {
            formatFileSize(downloadedSize) + " / " + formatFileSize(totalSize)
        }
    }
    
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
} 