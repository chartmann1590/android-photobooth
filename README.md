# Android Photobooth

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

A modern Android photobooth app built with Kotlin and Jetpack Compose. It provides a fullscreen photobooth experience with event-specific templates, a 3-second countdown with audible prompt, local saving, optional cloud upload, SMS/email sharing, a built-in gallery, and wireless printing via the Android print framework.

This project is designed to be used at events with minimal setup: configure your event template, SMS gateway, SMTP server, and optional upload backend, then run the photobooth in landscape mode.

## Features

- **Fullscreen Photobooth Experience** — Immersive landscape-only interface
- **3-Second Countdown** — Audible prompt and visual countdown timer
- **Event-Specific Templates** — Customizable photo frames and overlays
- **Local Photo Saving** — Automatic local storage of captured photos
- **Cloud Upload Support** — Optional Immich server integration
- **SMS Sharing** — Share photos via SMS gateway API
- **Email Sharing** — SMTP-based email delivery with attachments
- **Built-in Gallery** — View and share previously captured photos
- **Wireless Printing** — Android Print framework integration
- **Room Database** — Local persistence for photos and templates
- **DataStore Settings** — Modern key-value settings storage
- **Material 3 Design** — Beautiful, consistent UI theme

## Screenshots

Coming soon! 📸

## Requirements

- Android 7.0 (API level 24) or higher
- Camera permission
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

Configure Immich server for automatic cloud upload:

- **Immich URL** — Your Immich server URL
- **API Key** — Immich API key for authentication

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

## Usage

### Basic Photobooth Workflow

1. **Launch the App** — Open Android Photobooth on your device
2. **Configure Settings** — Set up your event, upload, and sharing preferences
3. **Navigate to Capture** — Tap "Start Photobooth" from the home screen
4. **Capture Photo** — Tap the capture button to start the countdown
5. **Review & Share** — View captured photos in the gallery and share via SMS, email, or print

### Gallery Features

- View all captured photos
- Share individual photos via SMS, email, or other apps
- Delete photos from local storage
- Print photos wirelessly

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
com.example.photobooth/
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
| Coil | 2.5.0 | Image loading in Compose |
| android-mail | 1.6.7 | SMTP email |
| Navigation Compose | 2.7.7 | Screen navigation |
| Print | 1.0.0 | Wireless printing |

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

- [ ] Add video capture support
- [ ] Implement additional templates
- [ ] Add social media sharing (Instagram, Twitter)
- [ ] Support for multiple cameras
- [ ] Animated countdown effects
- [ ] Custom watermark/logo support
- [ ] Real-time photo filters
- [ ] Photo booth booth mode (continuous capture)

## Support

If you encounter any issues or have questions, please open an issue on GitHub.

---

Made with ❤️ for events everywhere
