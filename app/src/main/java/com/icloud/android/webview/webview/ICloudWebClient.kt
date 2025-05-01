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
import com.icloud.android.webview.service.DownloadService

class ICloudWebClient(private val progressBar: ProgressBar) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // 处理文件下载请求
        val url = request?.url?.toString() ?: return false
        val contentDisposition = request.requestHeaders["Content-Disposition"]
        val mimeType = request.requestHeaders["Content-Type"]
        
        if (URLUtil.isNetworkUrl(url) && 
            (url.endsWith(".pdf") || url.endsWith(".doc") || url.endsWith(".docx") || 
             url.endsWith(".xls") || url.endsWith(".xlsx") || url.endsWith(".ppt") || 
             url.endsWith(".pptx") || url.endsWith(".zip") || url.endsWith(".rar") || 
             contentDisposition?.contains("attachment") == true)) {
                
            // 这是一个可能的文件下载链接
            downloadFile(view?.context, url, contentDisposition, mimeType)
            return true
        }
        
        // 保持所有其他URL在WebView中加载
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
                
                // 修改下载链接处理
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
        
        // 获取文件名
        var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        
        // 启动下载服务
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FILE_NAME, fileName)
        }
        context.startService(intent)
        
        Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
    }
} 