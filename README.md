# OfflineBeats — Android Audio Downloader

Downloader-only Android app for users who already use Samsung Music or another offline music player.

## Features
- Search both Wikimedia Commons and Internet Archive for audio whose metadata declares Public Domain, CC0, CC BY, or CC BY-SA (uploader metadata is not an independent rights guarantee).
- Find official artist/download pages using a web-search link when catalog results are empty. The app does not scrape or promise downloads from those pages.
- Proper screen insets on modern Samsung / Android 15+ phones.
- Download direct HTTPS links to MP3, M4A, AAC, FLAC, OGG, OPUS, or WAV files **when you have permission**.
- Receive links through Android's Share menu.
- Save downloaded files to the public **Music/OfflineBeats** folder for Samsung Music or other music apps.
- View download history, progress, source, creator, and license; open completed files in external audio apps.
- No ads, no account, no backend, no streaming or player code, no subscriptions.

## Not supported
YouTube or YouTube Music audio ripping, Spotify and other protected streaming services, scraping, paywall/DRM circumvention, or general web-page-to-MP3 conversion. YouTube links open the original page for official downloads where supported. YouTube's terms restrict downloading without explicit authorization from the service (and rights holders).

## Build with GitHub Actions
1. Open **Actions → Build Android APK** in this repository.
2. Choose **Run workflow** (or use the automatically triggered build for main).
3. When the job succeeds, download the **OfflineBeats-debug-apk** artifact.
4. Extract the ZIP and install **app-debug.apk** on Android 10+.
5. Search in **Samsung Music**; allow time for its media library to index Music/OfflineBeats.

No paid CI or server is necessary for this starter. The debug APK is not a Play Store production release. For release distribution, sign the APK with your own signing key.

## Stack
Kotlin, Jetpack Compose, Android DownloadManager, Wikimedia Commons Action API, Internet Archive search/metadata API, local SharedPreferences download history; JDK 17, Android SDK 35, Gradle 8.11.1, Android Gradle Plugin 8.9.2.

## What this does not solve
Searching for commercial artists like Honey Singh will not normally find authorized free audio downloads in public-domain/Creative-Commons catalogs. The artist-search link opens public web results for the user's own review, not a downloadable audio file. YouTube Premium downloads remain inside YouTube and cannot be exported as MP3 for Samsung Music.

## Known limitations
Direct audio URLs must end with a supported audio extension (query parameters are allowed). Remote servers may redirect or reject downloads. Android DownloadManager tracks status and puts files in Music/OfflineBeats, but it does not verify copyright, file format, or the safety of arbitrary downloads. Only download files you trust and have rights to use. Commons licenses may impose attribution and ShareAlike conditions.

## Source license
MIT (see LICENSE). Individual audio files have their own licenses; downloading does not change their terms.
