package com.icloud.android.webview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.icloud.android.webview.auth.AppleAuthHandler
import com.icloud.android.webview.auth.AppleAuthResult
import com.icloud.android.webview.databinding.ActivityMainBinding
import com.icloud.android.webview.service.ICloudMailService
import com.icloud.android.webview.webview.ICloudWebClient
import com.icloud.android.webview.webview.ICloudWebChromeClient
import com.icloud.android.webview.webview.JavaScriptInterface

class MainActivity : AppCompatActivity(), JavaScriptInterface.AppleLoginListener {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 1001
    private val STORAGE_PERMISSION_REQUEST_CODE = 1002
    private val iCloudUrl = "https://www.icloud.com/"
    
    private lateinit var jsInterface: JavaScriptInterface
    private lateinit var appleAuthHandler: AppleAuthHandler
    private lateinit var webClient: ICloudWebClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        
        // 初始化Apple授权处理器
        appleAuthHandler = AppleAuthHandler(this)
        
        setupWebView()
        setupSwipeRefresh()
        checkPermissions()
        startMailService()
        
        // 处理从Apple登录回调
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data?.scheme == "com.icloud.android.callback") {
                // 处理Apple授权回调
                val result = AppleAuthHandler.handleCallback(data)
                processAuthResult(result)
            }
        }
    }
    
    private fun processAuthResult(result: AppleAuthResult?) {
        if (result == null) return
        
        if (result.success && result.code != null) {
            // 授权成功，在WebView中完成登录
            Toast.makeText(this, getString(R.string.apple_login_success), Toast.LENGTH_SHORT).show()
            appleAuthHandler.completeLogin(binding.webView, result.code)
        } else {
            // 授权失败
            val errorMsg = result.error ?: getString(R.string.apple_login_cancelled)
            Toast.makeText(this, getString(R.string.apple_login_failed, errorMsg), Toast.LENGTH_LONG).show()
        }
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
        jsInterface = JavaScriptInterface(this)
        jsInterface.setAppleLoginListener(this) // 设置Apple登录监听器
        binding.webView.addJavascriptInterface(jsInterface, "Android")

        // 设置WebViewClient和WebChromeClient
        webClient = ICloudWebClient(binding.progressBar, appleAuthHandler)
        binding.webView.webViewClient = webClient
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
    
    // 实现 JavaScriptInterface.AppleLoginListener 接口
    override fun onAppleLoginRequested() {
        // 启动Apple登录
        val clientId = getString(R.string.apple_login_client_id)
        appleAuthHandler.startAuth(clientId)
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