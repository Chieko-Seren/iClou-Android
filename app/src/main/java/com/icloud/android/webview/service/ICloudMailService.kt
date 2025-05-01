package com.icloud.android.webview.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.core.app.NotificationManagerCompat
import com.icloud.android.webview.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ICloudMailService : Service() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var mailCheckJob: Job? = null
    private var webView: WebView? = null
    private val TAG = "ICloudMailService"
    private val CHECK_INTERVAL = 15 * 60 * 1000L // 15分钟检查一次

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        
        // 创建WebView用于后台检查
        initWebView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMailCheck()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initWebView() {
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
            }

            CookieManager.getInstance().apply {
                setAcceptThirdPartyCookies(this@apply, true)
                setAcceptCookie(true)
            }
        }
    }

    private fun startMailCheck() {
        mailCheckJob?.cancel()
        mailCheckJob = coroutineScope.launch {
            while (isActive) {
                try {
                    checkNewMail()
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking mail: ${e.message}", e)
                }
                delay(CHECK_INTERVAL)
            }
        }
    }

    private fun checkNewMail() {
        webView?.loadUrl("https://www.icloud.com/mail")
        
        // 注入JavaScript检查新邮件
        val mailCheckScript = """
            (function() {
                // 检查未读邮件数量
                const mailBadge = document.querySelector('.mail-badge');
                let count = 0;
                if (mailBadge && mailBadge.textContent) {
                    count = parseInt(mailBadge.textContent.trim()) || 0;
                }
                return count;
            })();
        """.trimIndent()
        
        webView?.evaluateJavascript(mailCheckScript) { result ->
            val count = result.toIntOrNull() ?: 0
            if (count > 0) {
                NotificationHelper.showNewMailNotification(this, count)
            }
        }
    }

    override fun onDestroy() {
        mailCheckJob?.cancel()
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
} 