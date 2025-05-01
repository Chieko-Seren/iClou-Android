package com.icloud.android.webview

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.icloud.android.webview.adapter.DownloadAdapter
import com.icloud.android.webview.databinding.ActivityDownloadManagerBinding
import com.icloud.android.webview.model.DownloadItem
import com.icloud.android.webview.service.DownloadService
import java.io.File

class DownloadManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadManagerBinding
    private lateinit var adapter: DownloadAdapter
    
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                DownloadService.DOWNLOAD_UPDATE, 
                DownloadService.DOWNLOAD_COMPLETE,
                DownloadService.DOWNLOAD_ERROR -> {
                    updateDownloadList()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
    }
    
    override fun onResume() {
        super.onResume()
        registerReceivers()
        updateDownloadList()
    }
    
    override fun onPause() {
        super.onPause()
        unregisterReceivers()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DownloadAdapter(
            onCancelClicked = { downloadItem ->
                cancelDownload(downloadItem)
            },
            onOpenClicked = { downloadItem ->
                openFile(downloadItem)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(DownloadService.DOWNLOAD_UPDATE)
            addAction(DownloadService.DOWNLOAD_COMPLETE)
            addAction(DownloadService.DOWNLOAD_ERROR)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadReceiver, filter)
    }
    
    private fun unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadReceiver)
    }
    
    private fun updateDownloadList() {
        val downloads = DownloadService.downloadList
        
        if (downloads.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.setDownloads(downloads)
        }
    }
    
    private fun cancelDownload(downloadItem: DownloadItem) {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadItem.id)
        }
        startService(intent)
    }
    
    private fun openFile(downloadItem: DownloadItem) {
        downloadItem.fileUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, getMimeType(downloadItem.fileName))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true) -> "application/msword"
            fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) -> "application/vnd.ms-excel"
            fileName.endsWith(".ppt", true) || fileName.endsWith(".pptx", true) -> "application/vnd.ms-powerpoint"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".gif", true) -> "image/gif"
            fileName.endsWith(".mp4", true) -> "video/mp4"
            fileName.endsWith(".mp3", true) -> "audio/mpeg"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".zip", true) -> "application/zip"
            else -> "*/*"
        }
    }
} 