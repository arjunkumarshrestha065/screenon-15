package com.example.signageplayer

import android.webkit.JavascriptInterface
import org.json.JSONArray

class AndroidBridge(
    private val storageManager: RemovableStorageManager,
    private val downloader: SignageStorageDownloader
) {

    @JavascriptInterface
    fun isRemovableStorageReady(): Boolean {
        return storageManager.isStorageReady()
    }

    @JavascriptInterface
    fun listMediaFiles(): String {
        val arr = JSONArray()
        storageManager.listFileNames().forEach { arr.put(it) }
        return arr.toString()
    }

    @JavascriptInterface
    fun deleteMediaFile(fileName: String): Boolean {
        if (!storageManager.isStorageReady()) return false
        return storageManager.deleteFile(fileName)
    }

    @JavascriptInterface
    fun downloadMediaFile(fileUrl: String, fileName: String): Boolean {
        if (!storageManager.isStorageReady()) return false
        return downloader.downloadToRemovableOnly(fileUrl, fileName)
    }

    @JavascriptInterface
    fun getLocalMediaUrl(fileName: String): String {
        val uri = storageManager.findFileUri(fileName) ?: return ""
        return uri.toString()
    }

    @JavascriptInterface
    fun getStorageStatusText(): String {
        return if (storageManager.isStorageReady()) {
            "Removable storage ready"
        } else {
            "Please connect/select removable storage. Media download is paused."
        }
    }
}
