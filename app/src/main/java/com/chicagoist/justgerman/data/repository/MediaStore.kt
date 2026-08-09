/*
 * Just German — учебный проект
 * Copyright (c) 2026 chicagoist
 *
 * SPDX-License-Identifier: MIT
 *
 * Аудио, учебник и метод Assimil принадлежат правообладателю Assimil SAS
 * (Франция, assimil.com). Распространение этих материалов запрещено.
 * См. файл NOTICE.
 */

package com.chicagoist.justgerman.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.util.zip.ZipInputStream

/**
 * Resolves media files (audio tracks and the textbook PDF) that may either be
 * bundled inside the APK assets (full local builds) or imported by the user
 * from a `resources.zip` archive (see README.md).
 *
 * Imported files take priority; bundled assets are the fallback.
 */
class MediaStore(private val context: Context) {

    val resourcesDir: File = File(context.filesDir, "resources")

    /** True when the given relative path exists in the bundled APK assets. */
    fun assetExists(relativePath: String): Boolean = try {
        context.assets.open(relativePath).close()
        true
    } catch (e: Exception) {
        false
    }

    fun hasBundledMedia(): Boolean = assetExists("resources/Assimil_DE.pdf")

    fun importedMediaCount(): Int =
        resourcesDir.walkTopDown().count {
            it.isFile && (it.extension.equals("mp3", true) || it.extension.equals("pdf", true))
        }

    /**
     * Approximate cap on cached extracted MP3 (~5 MB each → ~100 MB total).
     * Keeps the on-disk footprint bounded instead of growing to all 86 lessons
     * (~430 MB on top of a 430 MB APK).
     */
    private val audioCacheFileLimit = 20

    /**
     * Resolves a URL-encoded audio path from lessons.json
     * (e.g. "/resources/CD1/01%20Lektion.mp3") to a playable [Uri].
     *
     * Bundled MP3 assets are copied into [filesDir] on first use and returned
     * as a regular `file://` [Uri]. This avoids ExoPlayer's well-known issues
     * with the `asset:///` scheme on compressed APK assets and on filenames
     * that contain spaces (e.g. "01 Lektion.mp3"), which would otherwise
     * leave the player stuck in STATE_IDLE with no audible output and a
     * frozen progress bar.
     *
     * @return a `file://` URI for an imported or cached track, or null when
     *         the file is not available at all.
     */
    fun resolveAudioUri(encodedPath: String): Uri? {
        val file = resolveAudioFile(encodedPath) ?: return null
        return Uri.fromFile(file)
    }

    /**
     * Same resolution as [resolveAudioUri] but returns a [File].
     * Preference order: imported file → cached bundled copy → extract from assets.
     */
    fun resolveAudioFile(encodedPath: String): File? {
        val decoded = URLDecoder.decode(encodedPath, "UTF-8").removePrefix("/")
        val relative = decoded.removePrefix("resources/")
        val imported = File(resourcesDir, relative)
        if (imported.exists()) return imported
        val cached = File(context.filesDir, "audio/$relative")
        if (cached.exists()) {
            // Touch mtime so pruneAudioCache evicts by access order, not by
            // extract order — cheaper than tracking a separate LRU map.
            cached.setLastModified(System.currentTimeMillis())
            return cached
        }
        if (!assetExists(decoded)) return null
        val materialized = materializeAsset(decoded, cached) ?: return null
        pruneAudioCache()
        return materialized
    }

    /**
     * Returns the textbook PDF as a File suitable for FileProvider,
     * preferring the imported copy and falling back to the bundled asset
     * (copied into the app cache).
     */
    fun resolvePdfFile(): File? {
        val imported = File(resourcesDir, "Assimil_DE.pdf")
        if (imported.exists()) return imported
        if (!assetExists("resources/Assimil_DE.pdf")) return null
        val cached = File(context.cacheDir, "pdfs/Assimil_DE.pdf")
        if (cached.exists()) return cached
        return materializeAsset("resources/Assimil_DE.pdf", cached)
    }

    /**
     * Single helper used by both audio and PDF extraction. Centralises the
     * directory creation + copy + IOException handling so the two flows stay
     * in sync.
     */
    private fun materializeAsset(assetPath: String, target: File): File? {
        target.parentFile?.mkdirs()
        return try {
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target
        } catch (e: IOException) {
            // Missing asset, full disk, or permission error → caller treats
            // the file as "media unavailable" rather than crashing the player
            // or the PDF renderer.
            null
        }
    }

    /**
     * Best-effort LRU prune of the bundled MP3 cache. Runs after every copy
     * and keeps files around that were most recently opened. Touching mtime
     * in [resolveAudioFile] makes this real LRU and not "FIFO by extract".
     */
    private fun pruneAudioCache() {
        val dir = File(context.filesDir, "audio")
        if (!dir.isDirectory) return
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        if (files.size <= audioCacheFileLimit) return
        files.sortedBy { it.lastModified() }
            .take(files.size - audioCacheFileLimit)
            .forEach { runCatching { it.delete() } }
    }

    /**
     * Extracts media files (MP3 + PDF) from a resources.zip into app storage.
     *
     * The archive may be packed with or without a leading "resources/"
     * prefix; only .mp3 and .pdf entries are extracted (path traversal and
     * metadata files such as __MACOSX are skipped).
     *
     * @return the number of files written.
     */
    /**
     * Extracts media files (MP3 + PDF) from a resources.zip into app storage.
     *
     * @return the number of files written, or -1 when the archive could not be read.
     */
    fun importResourcesZip(uri: Uri): Int {
        resourcesDir.mkdirs()
        var count = 0
        val input = context.contentResolver.openInputStream(uri) ?: return -1
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = normalizeEntryName(entry.name)
                if (!entry.isDirectory && name != null) {
                    val target = File(resourcesDir, name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zip.copyTo(out) }
                    count++
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return count
    }

    private fun normalizeEntryName(raw: String): String? {
        var name = raw.replace('\\', '/')
        // Reject absolute paths and path traversal outright.
        if (name.startsWith("/") || name.contains("..")) return null
        // Accept archives packed with or without a leading resources/ prefix.
        val idx = name.lastIndexOf("resources/")
        if (idx >= 0) name = name.substring(idx + "resources/".length)
        val lower = name.lowercase()
        return if (lower.endsWith(".mp3") || lower.endsWith(".pdf")) name else null
    }
}
