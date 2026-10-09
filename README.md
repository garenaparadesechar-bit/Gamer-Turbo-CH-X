# Gamer Turbo CH X 2.0

Android app in Java, designed for a low/mid-range phone. Build using the GitHub Actions workflow `.github/workflows/build-apk.yml`.

## Implemented features
- Live RAM, battery percentage, and battery temperature where Android exposes it.
- Diagnostic advice based on Android memory-pressure flag and battery temperature.
- Clean the app's own cache and show estimated bytes removed.
- Launch Free Fire / Free Fire MAX if installed.
- Shizuku API dependency, binder/permission state, and actual privileged command execution after the user grants Shizuku permission.
- Request Android package-manager cache trimming with `pm trim-caches 1G` (Android chooses which cache files to remove; amount freed is not guaranteed).
- Set Android animation scales to 0.5x or restore to 1x through Shizuku.
- Choose a launchable app and force-stop it through Shizuku, with confirmation. This does not erase app data.
- Open display settings and this app's settings.

## Important limitations
- Shizuku must be separately installed and started by the user through Wireless debugging/ADB; this app cannot enable it by itself. The user must grant the app permission inside Shizuku.
- Cache trimming is controlled by Android and cannot guarantee a specific amount of space. Clearing this app's own cache does not clear other apps' caches.
- Android does not provide a general supported API for third-party apps to force CPU/GPU frequencies, guarantee lower temperatures, or promise higher game FPS. This app does not claim to do those things.
- No game files or account data are modified.

## Build
The workflow uses JDK 17, Android SDK platform 35, Gradle 8.7 and uploads the debug APK as artifact `Gamer-Turbo-CH-X`.
