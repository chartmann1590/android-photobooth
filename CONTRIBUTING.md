# Contributing to Android Photobooth

Thank you for your interest in contributing! This document provides guidelines for contributing to the Android Photobooth project.

## Development Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24 (min) to 34 (target/compile)
- JDK 17
- A physical Android device or emulator with camera support

### Build the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/android-photobooth.git
cd android-photobooth

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Project Structure

```
app/src/main/java/com/example/photobooth/
├── camera/          # CameraX integration and capture logic
├── data/            # Room database entities and DAOs
├── gallery/         # Gallery screen and view model
├── network/         # Image uploaders (Immich, Email, SMS)
├── settings/        # Settings persistence and models
├── template/        # Template definitions and rendering
└── ui/              # Screens, navigation, and theme
```

## Code Style

### Kotlin
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `val` over `var` when possible
- Prefer immutable data classes
- Use `StateFlow` for reactive state in ViewModels
- Use sealed interfaces for modeling UI states

### Jetpack Compose
- Follow [Compose best practices](https://developer.android.com/jetpack/compose/best-practices)
- Keep composables small and focused
- Pass state down, events up
- Use `@Preview` annotations for UI components

## Testing

### Unit Tests
Write unit tests for:
- ViewModels
- Repository classes
- Business logic
- Data transformations

```kotlin
@Test
fun `captureViewModel should start countdown when capture button clicked`() {
    // Test implementation
}
```

### UI Tests (Espresso + Compose UI Test)
Write UI tests for:
- Navigation flows
- User interactions
- Screen rendering

```kotlin
@Test
fun `captureScreen should display countdown after button click`() {
    // Test implementation
}
```

## Submitting Changes

### Branch Naming
- `feature/your-feature-name` - New features
- `fix/your-bug-fix` - Bug fixes
- `docs/your-doc-change` - Documentation updates
- `refactor/your-refactor` - Code refactoring

### Commit Messages
Follow conventional commits:
```
feat: add support for custom photo templates
fix: resolve crash when camera permission is denied
docs: update README with setup instructions
refactor: simplify CameraCaptureManager initialization
```

### Pull Request Process
1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Run all tests: `./gradlew test connectedAndroidTest`
5. Run code quality checks: `./gradlew lint`
6. Submit a pull request with a clear description

## Getting Help

- Check existing issues for similar problems
- Read the [CLAUDE.md](CLAUDE.md) for project-specific guidance
- Join discussions in the Issues section

## Project Guidelines

### Architecture
- MVVM pattern with Jetpack Compose
- No DI framework - use constructor injection
- Room for local data persistence
- DataStore Preferences for settings
- OkHttp for network requests

### Dependencies
- CameraX 1.3.3 for camera capture
- Room 2.6.1 for database
- DataStore Preferences 1.1.0 for settings
- Kotlinx.coroutines for async operations

Thank you for contributing! 🎉
