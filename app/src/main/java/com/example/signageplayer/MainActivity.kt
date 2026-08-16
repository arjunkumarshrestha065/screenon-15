package com.example.signageplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var storagePanel: LinearLayout
    private lateinit var storageStatusText: TextView
    private lateinit var selectStorageButton: Button

    private lateinit var storageManager: RemovableStorageManager
    private lateinit var downloader: SignageStorageDownloader
    private lateinit var bridge: AndroidBridge

    // IMPORTANT: Replace this URL with your real server player URL.
    private val playerUrl = "https://signage.yourdomain.com/player.html"

    companion object {
        private const val REQUEST_SELECT_STORAGE = 5001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        storagePanel = findViewById(R.id.storagePanel)
        storageStatusText = findViewById(R.id.storageStatusText)
        selectStorageButton = findViewById(R.id.selectStorageButton)

        storageManager = RemovableStorageManager(this)
        downloader = SignageStorageDownloader(this, storageManager)
        bridge = AndroidBridge(storageManager, downloader)

        setupWebView()

        selectStorageButton.setOnClickListener {
            val intent = storageManager.createSelectFolderIntent()
            startActivityForResult(intent, REQUEST_SELECT_STORAGE)
        }

        updateStorageStatus()
        reloadPlayer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SELECT_STORAGE && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                storageManager.saveSelectedFolder(uri)
                updateStorageStatus()
                reloadPlayer()
            } else {
                updateStorageStatus()
            }
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true

        WebView.setWebContentsDebuggingEnabled(false)
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(bridge, "AndroidBridge")
    }

    private fun reloadPlayer() {
        webView.loadUrl(playerUrl)
    }

    private fun updateStorageStatus() {
        if (storageManager.isStorageReady()) {
            storageStatusText.text = "Removable storage ready. Media will download only there."
            storagePanel.visibility = View.GONE
        } else {
            storageStatusText.text = "Please connect/select removable storage. Media download is paused."
            storagePanel.visibility = View.VISIBLE
        }
    }
}
