# OfflineBeats: Android downloader

A small **downloader-only** Android app. Samsung Music (or your preferred player) handles playback.

## Supports
- Search **Wikimedia Commons** audio with approved public-domain, CC0, CC BY, or CC BY-SA license metadata. Each download keeps creator, source, and license information in local history. Review attribution/ShareAlike requirements before redistributing.
- Download **direct HTTPS audio files** (.mp3, .m4a, .aac, .flac, .ogg, .opus, .wav) when you have permission. The app asks you to confirm rights.
- Receive shared HTTPS links from other apps.
- Keep downloads in `Music/OfflineBeats/` so the Android media scanner and Samsung Music can find them.
- Show queued/running/completed/failed downloads using Android DownloadManager; tap completed items to open with an installed audio app.

## Not supported
YouTube, YouTube Music, Spotify, SoundCloud streaming pages, DRM-protected audio, paywalled files, stream extraction, or arbitrary web-page-to-MP3 conversion. YouTube links can be opened in YouTube to use any **officially permitted** offline options. An uploader's permission alone does not grant an API or bypass a service's download restrictions.

## Build APK (no paid services)
1. Open **Actions → Build Android APK → Run workflow**.
2. Once tests and build succeed, open the workflow run and download the **OfflineBeats-debug-apk** artifact.
3. Extract and install `app-debug.apk` on an Android 10+ phone.
4. In Samsung Music, use its library scan/refresh if newly saved songs do not appear immediately.

To build locally: Android Studio or JDK 17, Android SDK 35, and Gradle 8.11.1: `gradle :app:assembleDebug`.

## Privacy & safety
No account, server, database, ads, analytics, storage scan, media library permissions, or music player. Downloads and source/credit history stay on the device. Downloads use system DownloadManager. Review links before downloading; the app does not inspect file contents and cannot grant rights.

## Licensing
Source code: Apache-2.0 (see LICENSE). Music files are licensed separately by their original creators.
