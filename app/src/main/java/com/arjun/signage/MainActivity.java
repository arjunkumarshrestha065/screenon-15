package com.arjun.signage;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int leftPressCount = 0;
    private long lastLeftPressTime = 0;
    private File webFolder;
    // Increase this number whenever bundled player.html/player.js is changed in APK.
    private static final int BUNDLED_WEB_VERSION = 200;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        hideSystemUI();

        webFolder = new File(getFilesDir(), "player_web");
        preparePlayerWebFiles(shouldRefreshBundledPlayer());

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        webView.addJavascriptInterface(new AndroidBridge(androidId), "AndroidBridge");
        loadPlayer();
    }


    private boolean shouldRefreshBundledPlayer() {
        try {
            File html = new File(webFolder, "player.html");
            File js = new File(webFolder, "player.js");
            File versionFile = new File(webFolder, "web_version.txt");

            if (!html.exists() || html.length() == 0) return true;
            if (!js.exists() || js.length() == 0) return true;
            if (!versionFile.exists()) return true;

            String savedVersionText = readText(versionFile, "0");
            int savedVersion = Integer.parseInt(savedVersionText.trim());
            return savedVersion < BUNDLED_WEB_VERSION;
        } catch (Exception e) {
            return true;
        }
    }

    private void preparePlayerWebFiles(boolean forceOverwrite) {
        try {
            if (!webFolder.exists()) webFolder.mkdirs();
            copyAssetToWeb("player.html", forceOverwrite);
            copyAssetToWeb("player.js", forceOverwrite);
            File version = new File(webFolder, "web_version.txt");
            if (!version.exists() || forceOverwrite) writeText(version, String.valueOf(BUNDLED_WEB_VERSION));
        } catch (Exception ignored) {}
    }

    private void copyAssetToWeb(String assetName, boolean forceOverwrite) throws Exception {
        File out = new File(webFolder, assetName);
        if (out.exists() && out.length() > 0 && !forceOverwrite) return;
        try (InputStream in = getAssets().open(assetName); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) fos.write(buffer, 0, read);
        }
    }

    private void writeText(File file, String text) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readText(File file, String fallback) {
        try {
            if (!file.exists()) return fallback;
            byte[] data = new byte[(int) file.length()];
            try (InputStream in = new java.io.FileInputStream(file)) {
                int read = in.read(data);
                if (read <= 0) return fallback;
            }
            return new String(data, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return fallback;
        }
    }

    private void loadPlayer() {
        File html = new File(webFolder, "player.html");
        webView.loadUrl("file://" + html.getAbsolutePath());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onBackPressed() {
        showPinMenu();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            // Normal exit keys will not exit directly. They open the PIN menu.
            if (keyCode == KeyEvent.KEYCODE_BACK
                    || keyCode == KeyEvent.KEYCODE_MENU
                    || keyCode == KeyEvent.KEYCODE_APP_SWITCH
                    || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                showPinMenu();
                return true;
            }

            // TV remote secret method: press LEFT 5 times within 3 seconds.
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                long now = System.currentTimeMillis();
                if (now - lastLeftPressTime > 3000) {
                    leftPressCount = 0;
                }
                leftPressCount++;
                lastLeftPressTime = now;

                if (leftPressCount >= 5) {
                    leftPressCount = 0;
                    showPinMenu();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void showPinMenu() {
        if (webView != null) {
            webView.evaluateJavascript("if(window.showAdminMenu){window.showAdminMenu();}", null);
        }
    }

    private File getBestMediaFolder() {
        File[] dirs = getExternalFilesDirs(null);
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir != null) {
                    try {
                        if (Environment.isExternalStorageRemovable(dir)) {
                            File folder = new File(dir, "media");
                            if (!folder.exists()) folder.mkdirs();
                            if (folder.exists() && folder.canWrite()) return folder;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        File external = getExternalFilesDir(null);
        if (external != null) {
            File folder = new File(external, "media");
            if (!folder.exists()) folder.mkdirs();
            if (folder.exists() && folder.canWrite()) return folder;
        }
        File fallback = new File(getFilesDir(), "media");
        if (!fallback.exists()) fallback.mkdirs();
        return fallback;
    }

    private String safeFileName(String name) {
        if (name == null || name.trim().length() == 0) return "media_file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void callJs(String script) {
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    private String downloadText(String fileUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.connect();
            if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
                throw new Exception("HTTP " + conn.getResponseCode());
            }
            try (InputStream in = conn.getInputStream()) {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) baos.write(buffer, 0, read);
                return baos.toString("UTF-8");
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public class AndroidBridge {
        private final String deviceId;

        AndroidBridge(String id) {
            deviceId = id;
        }

        @JavascriptInterface
        public String getDeviceId() {
            return deviceId;
        }

        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(() -> {
                try { finishAndRemoveTask(); } catch (Exception e) { finish(); }
            });
        }

        @JavascriptInterface
        public int getWebVersion() {
            try {
                return Integer.parseInt(readText(new File(webFolder, "web_version.txt"), "1"));
            } catch (Exception e) {
                return 1;
            }
        }

        @JavascriptInterface
        public void resetBundledPlayer() {
            runOnUiThread(() -> {
                preparePlayerWebFiles(true);
                loadPlayer();
            });
        }

        @JavascriptInterface
        public void reloadPlayer() {
            runOnUiThread(() -> loadPlayer());
        }

        @JavascriptInterface
        public void downloadWebUpdateAsync(String updateId, int version, String htmlUrl, String jsUrl) {
            executor.execute(() -> {
                JSONObject result = new JSONObject();
                try {
                    String html = downloadText(htmlUrl);
                    String js = downloadText(jsUrl);

                    if (!html.contains("player.js")) throw new Exception("Invalid player.html");
                    if (!js.contains("function") && !js.contains("const")) throw new Exception("Invalid player.js");

                    File htmlTmp = new File(webFolder, "player.html.tmp");
                    File jsTmp = new File(webFolder, "player.js.tmp");
                    writeText(htmlTmp, html);
                    writeText(jsTmp, js);

                    File htmlFile = new File(webFolder, "player.html");
                    File jsFile = new File(webFolder, "player.js");
                    if (htmlFile.exists()) htmlFile.delete();
                    if (jsFile.exists()) jsFile.delete();
                    htmlTmp.renameTo(htmlFile);
                    jsTmp.renameTo(jsFile);
                    writeText(new File(webFolder, "web_version.txt"), String.valueOf(version));

                    result.put("success", true);
                    result.put("version", version);
                } catch (Exception e) {
                    try { result.put("success", false); result.put("error", e.getMessage()); } catch (Exception ignored) {}
                }
                callJs("window.onWebUpdateComplete(" + JSONObject.quote(updateId) + "," + result.toString() + ")");
            });
        }

        @JavascriptInterface
        public String getStorageInfo() {
            try {
                File folder = getBestMediaFolder();
                JSONObject json = new JSONObject();
                json.put("path", folder.getAbsolutePath());
                json.put("freeBytes", folder.getFreeSpace());
                json.put("isRemovable", Environment.isExternalStorageRemovable(folder));
                return json.toString();
            } catch (Exception e) {
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }

        @JavascriptInterface
        public String fileExists(String fileName) {
            try {
                File folder = getBestMediaFolder();
                File file = new File(folder, safeFileName(fileName));
                JSONObject json = new JSONObject();
                json.put("exists", file.exists() && file.length() > 0);
                json.put("path", file.getAbsolutePath());
                json.put("localUrl", "file://" + file.getAbsolutePath());
                return json.toString();
            } catch (Exception e) {
                return "{\"exists\":false}";
            }
        }

        @JavascriptInterface
        public void downloadMediaAsync(String downloadId, String mediaUrl, String fileName) {
            executor.execute(() -> {
                JSONObject result = new JSONObject();
                HttpURLConnection conn = null;
                try {
                    File folder = getBestMediaFolder();
                    String safe = safeFileName(fileName);
                    File out = new File(folder, safe);

                    if (out.exists() && out.length() > 0) {
                        result.put("success", true);
                        result.put("fileName", safe);
                        result.put("path", out.getAbsolutePath());
                        result.put("localUrl", "file://" + out.getAbsolutePath());
                        result.put("alreadyExists", true);
                        callJs("window.onAndroidDownloadComplete(" + JSONObject.quote(downloadId) + "," + result.toString() + ")");
                        return;
                    }

                    conn = (HttpURLConnection) new URL(mediaUrl).openConnection();
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(120000);
                    conn.connect();

                    int total = conn.getContentLength();
                    long done = 0;
                    int last = 0;

                    try (InputStream in = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                            done += read;
                            if (total > 0) {
                                int percent = (int) ((done * 100) / total);
                                if (percent >= last + 3 || percent == 100) {
                                    last = percent;
                                    callJs("window.onAndroidDownloadProgress(" + JSONObject.quote(downloadId) + "," + percent + "," + JSONObject.quote(safe) + ")");
                                }
                            }
                        }
                    }

                    result.put("success", true);
                    result.put("fileName", safe);
                    result.put("path", out.getAbsolutePath());
                    result.put("localUrl", "file://" + out.getAbsolutePath());
                    result.put("isRemovable", Environment.isExternalStorageRemovable(folder));
                } catch (Exception e) {
                    try { result.put("success", false); result.put("error", e.getMessage()); } catch (Exception ignored) {}
                } finally {
                    if (conn != null) conn.disconnect();
                }
                callJs("window.onAndroidDownloadComplete(" + JSONObject.quote(downloadId) + "," + result.toString() + ")");
            });
        }
    }
}
