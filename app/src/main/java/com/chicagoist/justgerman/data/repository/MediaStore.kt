package com.chicagoist.justgerman.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
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
     * Resolves a URL-encoded audio path from lessons.json
     * (e.g. "/resources/CD1/01%20Lektion.mp3") to a playable URI.
     *
     * @return a file:// URI for an imported track, an asset:// URI for a
     *         bundled track, or null when the file is not available at all.
     */
    fun resolveAudioUri(encodedPath: String): Uri? {
        val decoded = URLDecoder.decode(encodedPath, "UTF-8").removePrefix("/")
        val imported = File(resourcesDir, decoded.removePrefix("resources/"))
        return when {
            imported.exists() -> Uri.fromFile(imported)
            assetExists(decoded) -> Uri.parse("asset:///$decoded")
            else -> null
        }
    }

    /**
     * Returns the textbook PDF as a File suitable for FileProvider,
     * preferring the imported copy and falling back to the bundled asset
     * (copied into the app cache).
     */
    fun resolvePdfFile(): File? {
        val imported = File(resourcesDir, "Assimil_DE.pdf")
        if (imported.exists()) return imported
        if (assetExists("resources/Assimil_DE.pdf")) {
            val cached = File(context.cacheDir, "pdfs/Assimil_DE.pdf")
            if (!cached.exists()) {
                cached.parentFile?.mkdirs()
                context.assets.open("resources/Assimil_DE.pdf").use { input ->
                    cached.outputStream().use { output -> input.copyTo(output) }
                }
            }
            return cached
        }
        return null
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
