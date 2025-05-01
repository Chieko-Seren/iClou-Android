package com.icloud.android.webview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.icloud.android.webview.databinding.ActivityMainBinding
import com.icloud.android.webview.service.ICloudMailService
import com.icloud.android.webview.webview.ICloudWebClient
import com.icloud.android.webview.webview.ICloudWebChromeClient
import com.icloud.android.webview.webview.JavaScriptInterface

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 1001
    private val STORAGE_PERMISSION_REQUEST_CODE = 1002
    private val iCloudUrl = "https://www.icloud.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupWebView()
        setupSwipeRefresh()
        checkPermissions()
        startMailService()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_downloads -> {
                openDownloadManager()
                true
            }
            R.id.action_refresh -> {
                binding.webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupWebView() {
        // 启用JavaScript和DOM存储
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        }

        // 允许第三方Cookie
        CookieManager.getInstance().apply {
            setAcceptThirdPartyCookies(binding.webView, true)
            setAcceptCookie(true)
        }

        // 添加JavaScript接口
        binding.webView.addJavascriptInterface(JavaScriptInterface(this), "Android")

        // 设置WebViewClient和WebChromeClient
        binding.webView.webViewClient = ICloudWebClient(binding.progressBar)
        binding.webView.webChromeClient = ICloudWebChromeClient()

        binding.webView.loadUrl(iCloudUrl)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // 通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // 存储权限 (Android 10以下)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startMailService() {
        val intent = Intent(this, ICloudMailService::class.java)
        startService(intent)
    }
    
    private fun openDownloadManager() {
        val intent = Intent(this, DownloadManagerActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE || requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要请求的权限才能正常使用应用", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
} 