package com.icloud.android.webview.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.icloud.android.webview.R
import com.icloud.android.webview.model.DownloadItem
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadService : Service() {
    
    companion object {
        const val ACTION_START_DOWNLOAD = "com.icloud.android.webview.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.icloud.android.webview.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        
        const val DOWNLOAD_UPDATE = "com.icloud.android.webview.DOWNLOAD_UPDATE"
        const val DOWNLOAD_COMPLETE = "com.icloud.android.webview.DOWNLOAD_COMPLETE"
        const val DOWNLOAD_ERROR = "com.icloud.android.webview.DOWNLOAD_ERROR"
        
        const val NOTIFICATION_CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1
        
        val downloadList = mutableListOf<DownloadItem>()
        private val activeDownloads = mutableMapOf<String, Boolean>()
        
        fun getDownloadByUrl(url: String): DownloadItem? {
            return downloadList.find { it.url == url }
        }
    }
    
    private val executor = Executors.newFixedThreadPool(3)
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        notificationBuilder = createNotificationBuilder()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示文件下载进度"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("文件下载")
            .setContentText("下载中...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: getFileNameFromUrl(url)
                
                // 检查是否已经在下载
                if (activeDownloads[url] == true) {
                    return START_STICKY
                }
                
                val downloadId = UUID.randomUUID().toString()
                val downloadItem = DownloadItem(
                    id = downloadId,
                    url = url,
                    fileName = fileName,
                    downloadedSize = 0L,
                    totalSize = 0L,
                    progress = 0,
                    status = DownloadItem.STATUS_PENDING,
                    timeStarted = Date()
                )
                
                downloadList.add(downloadItem)
                activeDownloads[url] = true
                
                // 更新UI
                sendBroadcast(DOWNLOAD_UPDATE, downloadItem)
                
                // 启动下载
                startDownload(downloadItem)
                
                return START_STICKY
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_NOT_STICKY
                cancelDownload(downloadId)
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }
    
    private fun startDownload(downloadItem: DownloadItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 更新状态
                downloadItem.status = DownloadItem.STATUS_DOWNLOADING
                sendBroadcast(DOWNLOAD_UPDATE, downloadItem)
                
                // 创建下载目录
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val iCloudDir = File(downloadDir, "iCloud")
                if (!iCloudDir.exists()) {
                    iCloudDir.mkdirs()
                }
                
                val file = File(iCloudDir, downloadItem.fileName)
                
                // 开始下载
                val url = URL(downloadItem.url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    downloadItem.status = DownloadItem.STATUS_FAILED
                    sendBroadcast(DOWNLOAD_ERROR, downloadItem)
                    return@launch
                }
                
                val totalSize = connection.contentLength.toLong()
                downloadItem.totalSize = totalSize
                
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var downloadedSize = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (activeDownloads[downloadItem.url] != true) {
                        // 下载被取消
                        inputStream.close()
                        outputStream.close()
                        file.delete()
                        return@launch
                    }
                    
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    
                    // 更新进度
                    downloadItem.downloadedSize = downloadedSize
                    downloadItem.progress = if (totalSize > 0) {
                        ((downloadedSize * 100) / totalSize).toInt()
                    } else {
                        0
                    }
                    
                    // 更新通知
                    updateNotification(downloadItem)
                    
                    // 发送广播更新UI
                    if (downloadItem.progress % 5 == 0) {
                        sendBroadcast(DOWNLOAD_UPDATE, downloadItem)
                    }
                }
                
                inputStream.close()
                outputStream.close()
                
                // 下载完成
                downloadItem.status = DownloadItem.STATUS_COMPLETED
                downloadItem.progress = 100
                sendBroadcast(DOWNLOAD_COMPLETE, downloadItem)
                
                // 更新文件URI
                downloadItem.fileUri = FileProvider.getUriForFile(
                    applicationContext,
                    "${applicationContext.packageName}.fileprovider",
                    file
                ).toString()
                
                // 完成通知
                showCompletionNotification(downloadItem)
                
            } catch (e: Exception) {
                e.printStackTrace()
                downloadItem.status = DownloadItem.STATUS_FAILED
                sendBroadcast(DOWNLOAD_ERROR, downloadItem)
            } finally {
                activeDownloads.remove(downloadItem.url)
            }
        }
    }
    
    private fun cancelDownload(downloadId: String) {
        val downloadItem = downloadList.find { it.id == downloadId } ?: return
        activeDownloads.remove(downloadItem.url)
        downloadItem.status = DownloadItem.STATUS_CANCELLED
        sendBroadcast(DOWNLOAD_UPDATE, downloadItem)
    }
    
    private suspend fun updateNotification(downloadItem: DownloadItem) = withContext(Dispatchers.Main) {
        val notification = notificationBuilder
            .setContentTitle("下载中: ${downloadItem.fileName}")
            .setContentText("${downloadItem.progress}%")
            .setProgress(100, downloadItem.progress, false)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private suspend fun showCompletionNotification(downloadItem: DownloadItem) = withContext(Dispatchers.Main) {
        val notification = NotificationCompat.Builder(this@DownloadService, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_complete)
            .setContentTitle("下载完成")
            .setContentText(downloadItem.fileName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(downloadItem.id.hashCode(), notification)
    }
    
    private fun sendBroadcast(action: String, downloadItem: DownloadItem) {
        val intent = Intent(action).apply {
            putExtra(EXTRA_DOWNLOAD_ID, downloadItem.id)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun getFileNameFromUrl(url: String): String {
        val lastPathSegment = url.substringAfterLast('/')
        return if (lastPathSegment.isNotEmpty()) {
            if (lastPathSegment.contains('?')) {
                lastPathSegment.substringBefore('?')
            } else {
                lastPathSegment
            }
        } else {
            "file_${System.currentTimeMillis()}"
        }
    }
} 