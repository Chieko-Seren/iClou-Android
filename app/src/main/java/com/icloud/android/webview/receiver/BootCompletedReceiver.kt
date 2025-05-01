package com.icloud.android.webview.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.icloud.android.webview.service.ICloudMailService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ICloudMailService::class.java)
            context.startService(serviceIntent)
        }
    }
} 