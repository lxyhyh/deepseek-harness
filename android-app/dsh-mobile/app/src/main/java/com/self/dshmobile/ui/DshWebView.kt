package com.self.dshmobile.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.self.dshmobile.BuildConfig

/**
 * dsh Web UI 嵌入视图（WebView）。
 * 加载容器内 `dsh web` 服务：http://127.0.0.1:3080（见方案 4/7.8）。
 * 对话 / 模型 / 思考强度 / 密钥 / plan / 子代理 / 技能等全部复用 dsh 网页主体。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DshWebView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    // 本机 dsh 服务之外的外部链接仍在 WebView 内处理，避免跳系统浏览器
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                }
                loadUrl("http://${BuildConfig.DSH_WEB_HOST}:${BuildConfig.DSH_WEB_PORT}")
            }
        }
    )
}
