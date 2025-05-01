package com.icloud.android.webview.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import org.json.JSONObject
import java.util.UUID

/**
 * 处理Apple第三方登录认证的工具类
 */
class AppleAuthHandler(private val context: Context) {

    companion object {
        private const val TAG = "AppleAuthHandler"
        private const val APPLE_AUTH_URL = "https://appleid.apple.com/auth/authorize"
        private const val CALLBACK_SCHEME = "com.icloud.android.callback"

        // 用于暂存认证状态
        private var authState: String? = null
        
        // 处理回调数据
        fun handleCallback(data: Uri?): AppleAuthResult? {
            if (data == null) return null
            
            try {
                val code = data.getQueryParameter("code")
                val state = data.getQueryParameter("state")
                val error = data.getQueryParameter("error")
                
                // 验证状态参数，防止CSRF攻击
                if (state != authState) {
                    Log.e(TAG, "State mismatch, possible CSRF attack")
                    return AppleAuthResult(success = false, error = "Security verification failed")
                }
                
                if (error != null) {
                    Log.e(TAG, "Authentication error: $error")
                    return AppleAuthResult(success = false, error = error)
                }
                
                if (code != null) {
                    // 清除authState
                    authState = null
                    return AppleAuthResult(success = true, code = code)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing callback data", e)
                return AppleAuthResult(success = false, error = "Error processing authentication callback data")
            }
            
            return AppleAuthResult(success = false, error = "Invalid callback data")
        }
    }
    
    /**
     * 启动Apple登录流程
     * @param clientId 客户端ID
     * @param redirectUri 重定向URI
     * @param scope 授权范围
     * @return 是否成功启动认证流程
     */
    fun startAuth(clientId: String, redirectUri: String = "$CALLBACK_SCHEME://callback", scope: String = "name email"): Boolean {
        try {
            // 生成随机状态参数，用于防止CSRF攻击
            authState = UUID.randomUUID().toString()
            
            // 构建认证URL
            val authUrlBuilder = Uri.parse(APPLE_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("state", authState)
                .appendQueryParameter("scope", scope)
                .appendQueryParameter("response_mode", "form_post")
            
            val authUrl = authUrlBuilder.build().toString()
            
            // 使用Chrome Custom Tabs打开认证页面
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(authUrl))
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authentication flow", e)
            return false
        }
    }
    
    /**
     * 注入JavaScript代码处理Apple登录按钮点击
     * @param webView WebView实例
     */
    fun injectAppleLoginHandler(webView: WebView) {
        val script = """
            (function() {
                // 监听页面上的Apple登录按钮
                function setupAppleLoginButtons() {
                    const appleButtons = document.querySelectorAll('button[aria-label*="Apple"], button[id*="apple"], a[href*="appleid.apple.com"]');
                    
                    if (appleButtons.length > 0) {
                        console.log('Found Apple login buttons, injecting handler');
                        
                        appleButtons.forEach(button => {
                            button.addEventListener('click', function(e) {
                                e.preventDefault();
                                e.stopPropagation();
                                
                                // 通知Android处理Apple登录
                                window.Android.handleAppleLogin();
                                
                                return false;
                            }, true);
                        });
                    }
                }
                
                // 页面加载完成时设置按钮
                setupAppleLoginButtons();
                
                // 使用MutationObserver监听DOM变化，处理动态加载的按钮
                const observer = new MutationObserver(function(mutations) {
                    setupAppleLoginButtons();
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script, null)
    }
    
    /**
     * 使用授权码完成登录
     * @param webView WebView实例
     * @param code 授权码
     */
    fun completeLogin(webView: WebView, code: String) {
        // 在WebView中设置授权码Cookie
        val script = """
            (function() {
                // 设置认证Cookie或localStorage数据
                try {
                    localStorage.setItem('apple_auth_code', '$code');
                    
                    // 触发自定义事件通知页面处理登录
                    const event = new CustomEvent('appleAuthComplete', { 
                        detail: { code: '$code' } 
                    });
                    document.dispatchEvent(event);
                    
                    return true;
                } catch(e) {
                    console.error('Error setting authorization data:', e);
                    return false;
                }
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script) { result ->
            Log.d(TAG, "Authorization code setting result: $result")
        }
    }
}

/**
 * Apple认证结果数据类
 */
data class AppleAuthResult(
    val success: Boolean,
    val code: String? = null,
    val error: String? = null
) 