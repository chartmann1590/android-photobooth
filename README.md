# Android Photobooth

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

A modern Android photobooth app built with Kotlin and Jetpack Compose. It provides a fullscreen photobooth experience with event-specific templates, a 3-second countdown with audible prompt, local saving, optional cloud upload, SMS/email sharing, short video capture, a built-in gallery, and wireless printing via the Android print framework.

This project is designed to be used at events with minimal setup: configure your event template, SMS gateway, SMTP server, and optional upload backend, then run the photobooth in landscape mode.

## Features

- **Fullscreen Photobooth Experience** — Immersive landscape-only interface
- **3-Second Countdown** — Color-cycling animated countdown with voice prompt and screen flash
- **Short Video Capture** — Optional 8-second MP4 clips with audio, in-gallery playback, upload, QR sharing, and Immich sync
- **Event-Specific Templates** — Customizable photo frames, overlays, and layout templates
- **Booth Mode** — Continuous capture mode with configurable photo count
- **Photo Filters** — 8 filters: Original, Grayscale, Sepia, B&W, Vintage, Cool, Warm, Vivid
- **Watermark/Logo** — Custom watermark with configurable position and opacity
- **Local Photo Saving** — Automatic local storage of captured photos
- **Cloud Upload Support** — Optional Immich server integration
- **SMS Sharing** — Share photos via SMS gateway API
- **Email Sharing** — SMTP-based email delivery with attachments
- **Social Media Sharing** — Android share sheet for Instagram, Twitter, WhatsApp, and more
- **Built-in Gallery** — View and share previously captured photos and videos
- **Wireless Printing** — Android Print framework integration
- **Multiple Cameras** — Support for all device cameras beyond front/back
- **Room Database** — Local persistence for photos and templates
- **DataStore Settings** — Modern key-value settings storage
- **Material 3 Design** — Beautiful, consistent UI theme

## Download

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=com.charles.photobooth)

Or build from source (see below).

## 🌐 Landing Page

Visit the landing page to learn more about the app: [**chartmann1590.github.io/android-photobooth**](https://chartmann1590.github.io/android-photobooth/)

## Requirements

- Android 7.0 (API level 24) or higher
- Camera permission
- Microphone permission (for video capture with audio)
- Internet connection (for cloud upload, SMS, and email features)
- Landscape orientation enforced

## Installation

### Build from Source

```bash
# Clone the repository
git clone https://github.com/chartmann1590/android-photobooth.git
cd android-photobooth

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The APK will be located in `app/build/outputs/apk/` (debug) or `app/build/outputs/apk/release/` (release).

### Install on Device

```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk
```

## Configuration

### Event Settings

Configure your photobooth event in the Settings screen:

- **Event Name** — Name of your event (e.g., "Wedding Reception")
- **Template** — Photo frame template to use
- **Countdown Duration** — Countdown timer in seconds (default: 3)

### Upload Settings (Optional)

Configure uploads for photos, GIFs, and videos:

- **Automatic Upload** - Upload captures immediately after saving when a destination is configured
- **Anonymous Host** - Generate public download links for QR code sharing

- **Immich URL** — Your Immich server URL
- **API Key** — Immich API key for authentication

- **Immich Album Sync** - Add uploaded captures to a configured Immich album

### SMS Settings (Optional)

Configure SMS gateway for photo sharing:

- **Gateway URL** — SMS gateway API endpoint
- **API Key** — SMS gateway authentication key
- **From Number** — Sender phone number

### SMTP Settings (Optional)

Configure email delivery:

- **SMTP Server** — SMTP server address
- **Port** — SMTP port (e.g., 587 for TLS)
- **Username** — SMTP authentication username
- **Password** — SMTP authentication password
- **From Email** — Sender email address
- **From Name** — Sender display name

### Video Capture Settings

Video capture is disabled by default and can be enabled in **Settings -> Capture Mode & Filters**:

- Videos are limited to 8 seconds.
- Videos include audio and require microphone permission.
- Videos can be uploaded, shared by QR code after upload, and synced to Immich.
- Videos cannot be printed, emailed, texted by SMS, or shared through the Android share sheet.

## Usage

### Basic Photobooth Workflow

1. **Launch the App** — Open Android Photobooth on your device
2. **Configure Settings** — Set up your event, upload, and sharing preferences
3. **Navigate to Capture** — Tap "Start Photobooth" from the home screen
4. **Capture Photo** — Tap the capture button to start the countdown
5. **Review & Share** — View captured photos in the gallery and share via SMS, email, or print

### Gallery Features

When video capture is enabled, guests can switch from photo mode to video mode on the capture screen. Videos skip photo-only template/filter/print/email/SMS actions and are handled through upload, QR sharing, Immich sync, playback, and delete.

- View captured photos, GIFs, and videos
- Upload photos, GIFs, and videos to generate QR codes
- Share photos and GIFs via SMS, email, Android apps, or print
- Delete captures from local storage (tap an item, then **Delete**)
- Videos are intentionally limited to upload, QR sharing, Immich sync, playback, and delete

### Where Are My Captures?

Captured photos are saved to app-specific external storage so a stock file
manager can find them without root:

```
Android/data/com.charles.photobooth/files/Pictures/
```

Captured videos are saved separately:

```
Android/data/com.charles.photobooth/files/Movies/
```

Open the system **Files** app, browse to that folder, and you can copy,
delete, or share the files directly. (Note: Android wipes these folders when
the app is uninstalled — back up before removing the app.)

### Multi-Photo Templates

The capture screen has a chip-row above the shutter for picking a layout:

- **Single** — one photo, full frame
- **2x2** — four photos composited into a 2x2 grid
- **Strip** — three photos stacked vertically as a photo strip

When you pick a multi-photo layout, the shutter button takes the required
number of shots back-to-back (with a countdown between each), then writes a
single composite JPEG to the gallery. You can also set a default in
**Settings → Capture Mode → Template Layout**.

## Architecture

This app uses modern Android development practices:

- **MVVM Architecture** — Model-View-ViewModel pattern with Jetpack Compose
- **CameraX** — Modern camera API integration
- **Room Database** — Local data persistence with Flow-based queries
- **DataStore Preferences** — Modern settings storage
- **OkHttp** — HTTP client for network requests
- **Kotlin Coroutines** — Asynchronous programming
- **Jetpack Compose** — Modern UI toolkit
- **Material 3** — Latest Material Design components

### Package Structure

```
com.charles.photobooth/
├── camera/          # CameraX integration and capture logic
├── data/            # Room database entities and DAOs
├── gallery/         # Gallery screen and view model
├── network/         # Image uploaders (Immich, Email, SMS)
├── settings/        # Settings persistence and models
├── template/        # Template definitions and rendering
└── ui/              # Screens, navigation, and theme
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| CameraX | 1.3.3 | Camera capture |
| Room | 2.6.1 | Local database |
| DataStore Preferences | 1.1.0 | Settings storage |
| OkHttp | 4.12.0 | HTTP client |
| kotlinx-serialization | 1.6.2 | JSON parsing |
| Coil | 2.6.0 | Image loading in Compose (with GIF + video-frame decoders) |
| android-mail | 1.6.7 | SMTP email |
| Navigation Compose | 2.7.7 | Screen navigation |
| Print | 1.0.0 | Wireless printing |
| Security Crypto | 1.1.0-alpha06 | Encrypted credential storage |
| Firebase BOM | 32.8.0 | Analytics, Crashlytics, Performance |

## Building and Testing

```bash
# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint

# Generate test coverage report
./gradlew jacocoTestReport
```

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [CameraX](https://developer.android.com/training/camerax) — Android camera library
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Modern UI toolkit
- [Room](https://developer.android.com/training/data-storage/room) — Database library
- [Immich](https://immich.app/) — Self-hosted photo management

## Author

Created by Charles Hartmann

## Roadmap

- [x] Implement additional templates (single portrait, 2x2 grid, vertical strip)
- [x] Add social media sharing (via Android share sheet)
- [x] Support for multiple cameras
- [x] Animated countdown effects (color-cycling, screen flash, booth counter)
- [x] Custom watermark/logo support
- [x] Photo filters (Grayscale, Sepia, B&W, Vintage, Cool, Warm, Vivid)
- [x] Photo booth booth mode (continuous capture)
- [x] GIF/Boomerang creation from burst captures
- [x] QR code sharing for instant guest download
- [x] Collage builder (multi-photo compositions)
- [x] Anonymous image hosting (storage.to / catbox.moe fallback)
- [x] Monetization system (free tier, rewarded ads, in-app purchase)
- [x] Interactive onboarding tutorial (19 steps)
- [x] GDPR-style consent dialog
- [x] Encrypted credential storage (AES256)
- [x] Firebase Analytics, Crashlytics & Performance Monitoring
- [x] PNG frame overlay upload system
- [ ] Remote trigger via Bluetooth/BLE shutter
- [ ] Green screen / background replacement
- [ ] Real-time live filter preview on camera feed
- [ ] Template editor with drag-and-drop layout
- [x] Full video capture UI (8-second videos, upload/QR sharing, Immich sync)
- [ ] Camera picker UI (select from all available cameras, not just front/back)

## Support

If you encounter any issues or have questions, please open an issue on GitHub.

## Sponsors

Support the development of this project:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-support-orange?logo=buymeacoffee&logoColor=white)](https://buymeacoffee.com/charleshartmann)

## Privacy Policy

Please read our [Privacy Policy](https://chartmann1590.github.io/android-photobooth/privacy.html) for information on how we handle your data.

---

Made with ❤️ for events everywhere
