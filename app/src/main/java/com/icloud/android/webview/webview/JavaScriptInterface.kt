package com.icloud.android.webview.webview

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.icloud.android.webview.util.NotificationHelper

class JavaScriptInterface(private val context: Context) {

    @JavascriptInterface
    fun onNewMail(count: String) {
        val mailCount = count.trim().toIntOrNull() ?: 0
        if (mailCount > 0) {
            NotificationHelper.showNewMailNotification(context, mailCount)
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
} 