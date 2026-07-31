package com.chicagoist.justgerman

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.InputStream

/**
 * Serves files from assets/www/.
 * Supports Next.js routes saved as directories (e.g. /lesson/1 -> www/lesson/1/index.html)
 * and exact static files (e.g. /_next/static/... -> www/_next/static/...).
 */
class SpaPathHandler(private val context: Context) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        val cleanPath = path.removePrefix("/")

        val candidates = mutableListOf<String>().apply {
            add("www/$cleanPath")
            add("www/$cleanPath/index.html")
            if (!cleanPath.endsWith(".html")) {
                add("www/$cleanPath.html")
            }
            if (cleanPath.isEmpty()) {
                add("www/index.html")
            }
        }

        var stream: InputStream? = null
        var matchedPath = ""

        for (candidate in candidates) {
            try {
                stream = context.assets.open(candidate)
                matchedPath = candidate
                break
            } catch (_: Exception) {
                // Try next candidate
            }
        }

        if (stream == null) return null

        val isText = matchedPath.endsWith(".html") ||
                matchedPath.endsWith(".js") ||
                matchedPath.endsWith(".css") ||
                matchedPath.endsWith(".json") ||
                matchedPath.endsWith(".svg")

        val mimeType = when {
            matchedPath.endsWith(".html") -> "text/html"
            matchedPath.endsWith(".js") -> "application/javascript"
            matchedPath.endsWith(".css") -> "text/css"
            matchedPath.endsWith(".json") -> "application/json"
            matchedPath.endsWith(".mp3") -> "audio/mpeg"
            matchedPath.endsWith(".pdf") -> "application/pdf"
            matchedPath.endsWith(".png") -> "image/png"
            matchedPath.endsWith(".jpg") || matchedPath.endsWith(".jpeg") -> "image/jpeg"
            matchedPath.endsWith(".svg") -> "image/svg+xml"
            matchedPath.endsWith(".ico") -> "image/x-icon"
            matchedPath.endsWith(".woff2") -> "font/woff2"
            else -> "application/octet-stream"
        }

        // Use null encoding for binary assets so WebView reads the raw bytes.
        val encoding = if (isText) "UTF-8" else null
        return WebResourceResponse(mimeType, encoding, stream)
    }
}
