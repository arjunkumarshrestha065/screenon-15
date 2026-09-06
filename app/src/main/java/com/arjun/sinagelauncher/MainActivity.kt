package com.arjun.sinagelauncher

// Explicit import to guarantee R resolves cleanly
import com.arjun.sinagelauncher.R

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
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

    // Change this to your server's player URL
    private val playerUrl = "https://google.com"

    companion object {
        private const val REQUEST_SELECT_STORAGE = 5001
        private const val REQUEST_CODE_ROLE_HOME = 6001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Keep display constantly ON (screen will not sleep)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Set layout
        setContentView(R.layout.activity_main)

        // 3. Bind views
        webView = findViewById(R.id.webView)
        storagePanel = findViewById(R.id.storagePanel)
        storageStatusText = findViewById(R.id.storageStatusText)
        selectStorageButton = findViewById(R.id.selectStorageButton)

        // 4. Initialize storage & downloader helpers
        storageManager = RemovableStorageManager(this)
        downloader = SignageStorageDownloader(this, storageManager)
        bridge = AndroidBridge(storageManager, downloader)

        // 5. Setup WebView and Storage Picker
        setupWebView()

        selectStorageButton.setOnClickListener {
            val intent = storageManager.createSelectFolderIntent()
            startActivityForResult(intent, REQUEST_SELECT_STORAGE)
        }

        // 6. Prompt user to set as Default Home App (Android 10+)
        promptSetDefaultHomeLauncher()

        updateStorageStatus()
        reloadPlayer()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableFullScreen()
        }
    }

    @Deprecated("Deprecated in Java")
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
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

    private fun promptSetDefaultHomeLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                startActivityForResult(intent, REQUEST_CODE_ROLE_HOME
