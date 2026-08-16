NATIVE APK WITH AUTORUN + ADMIN EXIT MENU

What is added:
1. Autorun after Android TV/mobile reboot.
2. Hidden admin menu.
3. Open Android Settings from APK.
4. Exit App from APK.
5. Reset Server / Re-pair option.
6. Restart Player option.
7. Offline playback fix remains.
8. Server play-time control, auto repeat and transitions remain.

Admin menu method:
- Tap the TOP-LEFT corner 5 times within 3 seconds.
- Enter PIN: 065065
- Options appear:
  - Reset Server / Re-pair
  - Open Android Settings
  - Restart Player
  - Exit App

Build method using GitHub:
1. Extract this ZIP.
2. Upload all extracted files/folders to GitHub repository root:
   - app/
   - build.gradle
   - settings.gradle
   - .github/workflows/build.yml
   - README_AUTORUN_ADMIN_MENU.txt
3. Go to GitHub Actions.
4. Run: Build Native Android APK
5. Download artifact: native-signage-player-debug-apk
6. Extract artifact and install app-debug.apk.

Important after install:
- Open app manually one time after install.
- Some Android boxes require allowing autostart in device settings.
- Some Android versions/manufacturers can block boot autorun if app is force-stopped or battery optimization blocks it.
- For best signage use, disable battery optimization for this app if available.

Server URL:
- Default URL is inside app/src/main/assets/player.js
- Change DEFAULT_SERVER_URL if needed.
