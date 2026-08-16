NATIVE ANDROID SIGNAGE PLAYER APK FILES

Upload these extracted files/folders to GitHub root:
- app/
- build.gradle
- settings.gradle
- .github/workflows/build.yml
- README_NATIVE_APK_METHOD.txt

Build method:
1. Create GitHub repository.
2. Upload all extracted files/folders. Do not upload only ZIP.
3. Go to Actions.
4. Click Build Native Android APK.
5. Click Run workflow.
6. Open latest successful build.
7. Download artifact native-signage-player-debug-apk.
8. Extract downloaded ZIP.
9. Install app-debug.apk on Android mobile/TV box.

If server URL changes:
Edit app/src/main/assets/player.js and change DEFAULT_SERVER_URL.

This APK supports:
- Pairing code
- Playlist play time from server
- Auto repeat
- Random transitions
- Background download
- Local storage/removable app-specific storage if available
- Reset Server button for re-pairing
