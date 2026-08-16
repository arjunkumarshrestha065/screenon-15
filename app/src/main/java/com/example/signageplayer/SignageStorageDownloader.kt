package com.example.signageplayer

import android.content.Context
import android.webkit.MimeTypeMap
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

class SignageStorageDownloader(
    private val context: Context,
    private val storageManager: RemovableStorageManager
) {

    fun downloadToRemovableOnly(fileUrl: String, fileName: String): Boolean {
        if (!storageManager.isStorageReady()) return false

        val targetUri = storageManager.createOrReplaceFile(fileName, guessMimeType(fileName)) ?: return false

        return try {
            val conn = URL(fileUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 120000

            if (conn.responseCode != 200) return false

            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                BufferedInputStream(conn.inputStream).use { input ->
                    val buffer = ByteArray(1024 * 64)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        if (!storageManager.isStorageReady()) {
                            return false
                        }

                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: return false

            true
        } catch (_: Exception) {
            try { storageManager.deleteFile(fileName) } catch (_: Exception) {}
            false
        }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', '').lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
