# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android photobooth app for events — Kotlin, Jetpack Compose, single-module. Forces landscape orientation. Designed for minimal setup: configure event template, SMS gateway, SMTP server, and optional upload backend, then run.

**Package:** `com.example.photobooth`
**Min SDK:** 24 · **Target/Compile SDK:** 34 · **Java:** 17 · **Kotlin Compiler Extension:** 1.5.8

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean build
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
```

## Architecture

**MVVM with Jetpack Compose.** Organized by feature under `app/src/main/java/com/example/photobooth/`.

### Feature Packages

- **`camera/`** — CameraX integration. `CameraCaptureManager` handles camera lifecycle and photo capture. `CaptureViewModel` manages capture state via sealed interfaces (`Idle`, `Countdown`, `Capturing`, `Saved`, `Error`).
- **`data/`** — Room database (`AppDatabase` singleton) with two entities: `PhotoEntity` and `TemplateEntity`, each with a DAO returning `Flow`-based queries.
- **`network/`** — `ImageUploader` interface with `ImmichUploader` (private Immich server) implementations. `SmtpEmailClient` for email with attachments. `SmsGatewayClient` for SMS via android-sms-gateway API.
- **`settings/`** — `SettingsRepository` persists configuration via DataStore Preferences. `SettingsModels` defines data classes for event, upload, SMS, and SMTP settings.
- **`template/`** — `TemplateDefinition` describes frame layouts with percentage-based positioning. `TemplateRenderer` composites captured bitmaps into final output with overlays.
- **`gallery/`** — `GalleryViewModel` manages photo gallery state and sharing actions.
- **`ui/`** — Navigation (`NavGraph` with sealed class routes), screens (`HomeScreen`, `CaptureScreen`, `GalleryScreen`, `SettingsScreen`), and Material 3 theme.

### Key Patterns

- **No DI framework** — dependencies passed via constructor injection
- **StateFlow** for reactive UI state in ViewModels with `viewModelScope` coroutines
- **Sealed interfaces** for UI state modeling (e.g., `CaptureUiState`)
- **Strategy pattern** for image uploaders (`ImageUploader` interface)
- **Room + DataStore** — Room for structured data (photos, templates), DataStore for key-value settings

### Navigation

Four screens: `Home → Capture → Gallery → Settings`, defined as sealed class routes in `NavGraph.kt`.

## Key Dependencies

| Library | Purpose |
|---------|---------|
| CameraX 1.3.3 | Camera capture |
| Room 2.6.1 | Local database (kapt processor) |
| DataStore Preferences 1.1.0 | Settings storage |
| OkHttp 4.12.0 | HTTP client |
| kotlinx-serialization 1.6.2 | JSON parsing |
| Coil 2.5.0 | Image loading in Compose |
| JavaMail (android-mail 1.6.7) | SMTP email |
| Navigation Compose 2.7.7 | Screen navigation |
| Print 1.0.0 | Wireless printing |

## Testing

JUnit 4 for unit tests, Espresso + Compose UI Test for instrumented tests. Test infrastructure is configured but tests are not yet written. Test runner: `AndroidJUnitRunner`.
