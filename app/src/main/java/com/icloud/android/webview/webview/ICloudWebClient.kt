package com.icloud.android.webview.webview

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar

class ICloudWebClient(private val progressBar: ProgressBar) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // 保持所有URL在WebView中加载
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        progressBar.visibility = View.VISIBLE
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        progressBar.visibility = View.GONE
        
        // 注入JavaScript检测邮件变化
        val mailCheckScript = """
            (function() {
                // 监听邮件通知元素变化
                const checkInterval = setInterval(function() {
                    const mailBadge = document.querySelector('.mail-badge');
                    if (mailBadge && mailBadge.textContent) {
                        window.Android.onNewMail(mailBadge.textContent);
                    }
                }, 10000); // 每10秒检查一次
            })();
        """.trimIndent()
        
        view?.evaluateJavascript(mailCheckScript, null)
    }
} 