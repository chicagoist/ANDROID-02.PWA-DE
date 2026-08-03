package com.chicagoist.justgerman

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import android.webkit.WebViewClient
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ASSET_PREFIX = "www"
        private const val PDF_BUTTON_SCRIPT = "<script src=\"/pdf-button.js\"></script>"
        private const val PROGRESS_MOCK_SCRIPT = "<script src=\"/progress-mock.js\"></script>"
        private const val HEAD_OPEN = "<head>"
        private const val BODY_CLOSE = "</body>"

        // Neutralizes the PWA service worker: it clears all caches (from old broken
        // builds) and unregisters itself, so it can never intercept or poison requests.
        private const val SW_STUB = "self.addEventListener('install',e=>self.skipWaiting());" +
            "self.addEventListener('activate',e=>e.waitUntil((async()=>{" +
            "const keys=await caches.keys();" +
            "await Promise.all(keys.map(k=>caches.delete(k)));" +
            "await self.registration.unregister();" +
            "})()));"
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            webViewClient = AppWebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    Log.d("JGWEBVIEW", "${msg.messageLevel()} ${msg.message()} @ ${msg.sourceId()}:${msg.lineNumber()}")
                    return true
                }
            }
            configureSettings()
        }
        setContentView(webView)

        if (savedInstanceState == null) {
            webView.loadUrl("https://appassets.androidplatform.net/")
        }
    }

    private fun WebView.configureSettings() {
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            // Never reuse a stale HTTP cache — everything is served from local assets,
            // and old cached responses from previous builds poisoned the app.
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
    }

    private fun mimeType(fileName: String): String = when {
        fileName.endsWith(".html") || fileName.endsWith(".htm") -> "text/html"
        fileName.endsWith(".js")  || fileName.endsWith(".mjs") -> "application/javascript"
        fileName.endsWith(".css") -> "text/css"
        fileName.endsWith(".json") -> "application/json"
        fileName.endsWith(".mp3") -> "audio/mpeg"
        fileName.endsWith(".m4a") -> "audio/mp4"
        fileName.endsWith(".ogg") -> "audio/ogg"
        fileName.endsWith(".wav") -> "audio/wav"
        fileName.endsWith(".pdf") -> "application/pdf"
        fileName.endsWith(".png") -> "image/png"
        fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
        fileName.endsWith(".svg") -> "image/svg+xml"
        fileName.endsWith(".ico") -> "image/x-icon"
        fileName.endsWith(".woff2") -> "font/woff2"
        fileName.endsWith(".woff") -> "font/woff"
        fileName.endsWith(".webmanifest") -> "application/manifest+json"
        fileName.endsWith(".xml") -> "application/xml"
        else -> "application/octet-stream"
    }

    // Injects progress-mock.js early (right after <head>) so window.fetch is patched
    // before the app code runs, and pdf-button.js at the end so the button appears.
    private fun injectScripts(input: java.io.InputStream): ByteArrayInputStream {
        val content = input.readBytes().toString(Charsets.UTF_8)
        val withProgressMock = if (content.contains(HEAD_OPEN)) {
            content.replaceFirst(HEAD_OPEN, "$HEAD_OPEN$PROGRESS_MOCK_SCRIPT")
        } else {
            PROGRESS_MOCK_SCRIPT + content
        }
        val injected = if (withProgressMock.contains(BODY_CLOSE)) {
            withProgressMock.replace(BODY_CLOSE, "$PDF_BUTTON_SCRIPT$BODY_CLOSE")
        } else {
            withProgressMock + PDF_BUTTON_SCRIPT
        }
        return ByteArrayInputStream(injected.toByteArray(Charsets.UTF_8))
    }

    // Serves files from assets/www/{path}, with SPA fallback for non-asset routes
    private fun serveAsset(path: String): WebResourceResponse? {
        val cleanPath = path.removePrefix("/")
        val assetPath = "$ASSET_PREFIX/$cleanPath"

        // Replace the real PWA service worker with an inert stub — it clears old
        // caches and unregisters, preventing cache poisoning from previous builds.
        if (cleanPath == "sw.js" || cleanPath.startsWith("swe-worker")) {
            return WebResourceResponse(
                "application/javascript", "UTF-8",
                ByteArrayInputStream(SW_STUB.toByteArray(Charsets.UTF_8))
            )
        }

        // Helper to serve a file (injects pdf-button.js + progress-mock.js into HTML)
        fun serveFile(p: String): WebResourceResponse {
            val type = mimeType(p)
            val input = assets.open(p)
            return if (type == "text/html") {
                WebResourceResponse(type, "UTF-8", injectScripts(input))
            } else {
                WebResourceResponse(type, "UTF-8", input)
            }
        }

        // 1. Try direct file
        try { return serveFile(assetPath) } catch (_: Exception) {}

        // 2. Try directory → index.html
        try { return serveFile("$assetPath/index.html") } catch (_: Exception) {}

        // 3. SPA fallback — only for navigation routes, not static assets
        if (cleanPath.startsWith("_next/") ||
            cleanPath.startsWith("icons/") ||
            cleanPath.startsWith("resources/") ||
            cleanPath == "manifest.json" ||
            cleanPath.startsWith("favicon.")
        ) return null

        return try {
            serveFile("$ASSET_PREFIX/index.html")
        } catch (_: Exception) {
            null
        }
    }

    inner class AppWebViewClient : WebViewClient() {

        private val assetLoader: WebViewAssetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/") { serveAsset(it) }
            .build()

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val path = request.url.path ?: ""

        if (path.endsWith(".pdf", ignoreCase = true)) {
                val page = request.url.fragment?.removePrefix("page=")?.toIntOrNull()
                    ?: request.url.getQueryParameter("page")?.toIntOrNull()
                openPdfFromAssets(path, page)
                return WebResourceResponse(
                    "text/html", "UTF-8",
                    ByteArrayInputStream("<html><body style='background:#111;color:#fff;'>Opening PDF…</body></html>".toByteArray())
                )
            }

            return assetLoader.shouldInterceptRequest(request.url)
                ?: super.shouldInterceptRequest(view, request)
        }
    }

    private fun openPdfFromAssets(path: String, page: Int? = null) {
        try {
            val assetPath = "$ASSET_PREFIX/${path.removePrefix("/")}"
            val input = assets.open(assetPath)

            val pdfDir = File(cacheDir, "pdfs").also { it.mkdirs() }
            val tempFile = File(pdfDir, "temp_${System.currentTimeMillis()}.pdf")
                .also { it.deleteOnExit() }
            FileOutputStream(tempFile).use { input.copyTo(it) }

            var uri = FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", tempFile
            )
            if (page != null) uri = Uri.parse("$uri#page=$page")

            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    },
                    "Open PDF"
                )
            )
        } catch (e: Exception) {
            Toast.makeText(this, "PDF error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
