# Text Launcher

Text Launcher is a minimal, distraction-free Android launcher for people who want their phone to feel calmer and more intentional. It replaces icon grids and engagement-heavy surfaces with a quiet text-first home screen, fast app search, lightweight planning prompts, and optional screen-time context.

The goal is simple: make the phone useful without making it irresistible.

## Features

- Text-first home screen with configurable app shortcuts.
- Fuzzy app search for quickly opening the app you meant to find.
- Optional date, analog/digital clock behavior, and quick access actions.
- Screen-time page with today usage, weekly usage, top-app recap, and daily average.
- App blocking prompts that slow down impulsive launches.
- Daily app budgets for selected apps.
- Intention tracking so each intentional app launch can be paired with a planned duration.
- Notes page for quick local notes.
- Calendar page for upcoming events, with selectable calendars.
- Settings for hiding or showing launcher pages and adjusting shortcut limits.
- Local-first storage using Android shared preferences.

## Philosophy

Text Launcher is built around friction with a purpose. It should still be easy to call someone, check your calendar, write a note, or open a tool you genuinely need. It should just be a little harder to fall into a loop you did not choose.

This project is intentionally small, plain, and hackable. It favors Android platform APIs and simple Kotlin over heavy framework choices.

## Screens and Permissions

Text Launcher can be set as your Android home app. Some features ask for optional permissions:

- Calendar access: used only to show upcoming calendar events in the launcher.
- Usage access: used only to calculate screen-time summaries and app budget prompts.

The app stores launcher settings, notes, shortcuts, app budgets, and intention data locally on the device. See `PRIVACY.md` for more detail.

## Requirements

- Android Studio or the Android command-line tools.
- JDK 17.
- Android SDK with compile SDK 36 installed.
- Android 8.0 or newer on device or emulator.

## Build

From the repository root:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```sh
./gradlew test
./gradlew assembleDebug
```

The debug APK will be generated under `app/build/outputs/apk/debug/`.

## Development

This is a Kotlin Android app using XML layouts and ViewBinding. The main app code lives under:

- `app/src/main/java/com/example/textlauncher/data`
- `app/src/main/java/com/example/textlauncher/domain`
- `app/src/main/java/com/example/textlauncher/ui`
- `app/src/main/res`

Run unit tests before opening a pull request:

```sh
./gradlew test
```

## Contributing

Contributions are welcome. Please read `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, and `PRIVACY.md` before opening issues or pull requests.

Good first areas include:

- Launcher ergonomics and accessibility.
- Better screen-time summaries.
- More tests around search, budget prompts, and settings persistence.
- Documentation and setup improvements.

## License

Text Launcher is open source under the MIT License. See `LICENSE` for details.
