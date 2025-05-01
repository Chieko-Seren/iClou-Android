package com.icloud.android.webview.webview

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import com.icloud.android.webview.auth.AppleAuthHandler
import com.icloud.android.webview.service.DownloadService

class ICloudWebClient(
    private val progressBar: ProgressBar,
    private val appleAuthHandler: AppleAuthHandler? = null
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        val contentDisposition = request.requestHeaders["Content-Disposition"]
        val mimeType = request.requestHeaders["Content-Type"]
        
        if (url.startsWith("https://appleid.apple.com/") || url.contains("apple.com/auth")) {
            appleAuthHandler?.let {
                val clientId = view?.context?.resources?.getString(
                    view.context.resources.getIdentifier("apple_login_client_id", "string", view.context.packageName)
                ) ?: return false
                
                it.startAuth(clientId)
                return true
            }
        }
        
        if (URLUtil.isNetworkUrl(url) && 
            (url.endsWith(".pdf") || url.endsWith(".doc") || url.endsWith(".docx") || 
             url.endsWith(".xls") || url.endsWith(".xlsx") || url.endsWith(".ppt") || 
             url.endsWith(".pptx") || url.endsWith(".zip") || url.endsWith(".rar") || 
             contentDisposition?.contains("attachment") == true)) {
                
            downloadFile(view?.context, url, contentDisposition, mimeType)
            return true
        }
        
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        progressBar.visibility = View.VISIBLE
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        progressBar.visibility = View.GONE
        
        appleAuthHandler?.injectAppleLoginHandler(view ?: return)
        
        val mailCheckScript = """
            (function() {
                const checkInterval = setInterval(function() {
                    const mailBadge = document.querySelector('.mail-badge');
                    if (mailBadge && mailBadge.textContent) {
                        window.Android.onNewMail(mailBadge.textContent);
                    }
                }, 10000);
                
                document.addEventListener('click', function(e) {
                    const target = e.target.closest('a');
                    if (target && target.href) {
                        const url = target.href;
                        if (url.endsWith('.pdf') || url.endsWith('.doc') || url.endsWith('.docx') || 
                            url.endsWith('.xls') || url.endsWith('.xlsx') || url.endsWith('.ppt') || 
                            url.endsWith('.pptx') || url.endsWith('.zip') || url.endsWith('.rar') ||
                            target.getAttribute('download')) {
                                
                            e.preventDefault();
                            window.Android.downloadFile(url, target.getAttribute('download') || '');
                            return false;
                        }
                    }
                }, true);
            })();
        """.trimIndent()
        
        view?.evaluateJavascript(mailCheckScript, null)
    }
    
    private fun downloadFile(context: android.content.Context?, url: String, contentDisposition: String?, mimeType: String?) {
        if (context == null) return
        
        var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILE_NAME, fileName)
        }
        context.startService(intent)
        
        Toast.makeText(context, "Started download: $fileName", Toast.LENGTH_SHORT).show()
    }
} 