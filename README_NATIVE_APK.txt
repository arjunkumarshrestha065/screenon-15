NATIVE ANDROID SIGNAGE PLAYER SOURCE

This source is for native Android APK build. It supports:
- Native WebView player.
- Server pairing code.
- Server playlist duration control.
- Auto repeat playlist.
- Random transition effects.
- Server download queue.
- Background media download.
- Small corner progress text.
- Save media to removable storage app folder if Android allows it.
- Fallback to internal app storage if removable storage is unavailable.

Default server URL included:
https://say-prompter-punctual.ngrok-free.dev

Important storage note:
Android does not normally allow apps to freely write to USB root.
This app tries to save in app-specific removable storage:
USB/Android/data/com.arjun.signage/files/media/
If that is not available/writable, it saves internally.

HOW TO BUILD APK WITHOUT ANDROID STUDIO:
1. Create a GitHub account.
2. Create a new repository, for example: native-signage-player
3. Upload all files/folders from this ZIP to the repository root:
   - app/
   - build.gradle
   - settings.gradle
   - .github/workflows/build.yml
4. Open GitHub repository > Actions.
5. Select "Build Native Android APK".
6. Click "Run workflow".
7. After build completes, open the latest workflow run.
8. Download artifact: native-signage-player-debug-apk
9. Extract it and install app-debug.apk on Android TV Box.

HOW TO USE APK:
1. Open app on TV box.
2. Server URL is already filled. Click Connect.
3. App shows pairing code.
4. Enter pairing code in CMS dashboard.
5. Assign playlist/media.
6. Device downloads in queue, saves locally, and plays local files.

FILES TO EDIT IF SERVER URL CHANGES:
app/src/main/assets/player.js
Change DEFAULT_SERVER_URL.
