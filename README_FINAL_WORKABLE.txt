FINAL WORKABLE SIGNAGE APK SOURCE

This rebuild fixes:
- Ads showing only file name: fixed video/image HTML output.
- Connect button no response: fixed button event binding.
- Reset button no response: fixed reset event binding and URL clear.
- PIN exit: Back/Menu/Escape opens PIN menu.
- Autostart: BOOT_COMPLETED + Home/Launcher mode included.
- Offline playback: keeps local playlist and local files.
- Random transitions: included.

Important setup after install:
1. Open app manually one time.
2. Press Home button.
3. Select Final Signage Player.
4. Choose Always.
5. Disable battery optimization if available.
6. Restart Android TV box.

Exit method:
- Back/Menu/Escape opens PIN menu.
- Tap top-left 5 times also opens PIN menu.
- PIN: 065065

Server URL:
- Change DEFAULT_SERVER_URL in app/src/main/assets/player.js to your PC local IP.
- Example: const DEFAULT_SERVER_URL = 'http://192.168.1.10:3000';
