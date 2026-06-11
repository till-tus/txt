# Developer Guide

This guide is for contributors working on Text Launcher. It covers the local setup, project structure, validation steps, and the public-readiness expectations for changes.

## Project Overview

Text Launcher is a native Android launcher written in Kotlin. It uses XML layouts with ViewBinding and keeps state local to the device through SharedPreferences and Android platform APIs.

The app intentionally has no analytics. Features that read personal device data, such as calendar events, approximate location, active notifications, and app usage, should remain optional, narrowly scoped, and easy to explain.

## Requirements

- JDK 17.
- Android Studio or Android command-line tools.
- Android SDK with compile SDK 36 installed.
- A device or emulator running Android 8.0 or newer.

## Repository Layout

- `app/src/main/java/com/example/textlauncher/data`: repositories for device data and local persistence.
- `app/src/main/java/com/example/textlauncher/domain`: small domain models and enums.
- `app/src/main/java/com/example/textlauncher/ui`: Activity, ViewModel, adapters, custom views, and UI controllers.
- `app/src/main/res/layout`: XML layouts.
- `app/src/main/res/values`: colors, dimensions, styles, and strings.
- `app/src/test`: local JVM tests.
- `docs/screenshots`: screenshots referenced by the README.

## Local Setup

1. Clone the repository.
2. Open it in Android Studio, or use the Gradle wrapper from the command line.
3. Let Gradle sync.
4. Confirm tests pass:

```sh
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

Build a debug APK:

```sh
./gradlew assembleDebug
```

## Architecture

`MainActivity` owns the launcher surface and Android interaction points. `HomeViewModel` coordinates UI state and delegates persistence or device queries to repository classes.

The current storage model is intentionally plain:

- `LauncherSettingsRepository`: preferences for visible pages, gestures, shortcut limits, app budgets, blocked apps, and selected calendar IDs.
- `ShortcutRepository`: chosen home-screen shortcuts.
- `NoteRepository`: quick notes.
- `TodayWidgetRepository`: Today page widget layout and widget configuration.
- `TodayNotificationCenter`: in-memory active-notification state for the Today notification widget.
- `AppUsageIntentionRepository`: daily intended app-use minutes.
- `CalendarRepository`: reads calendars and upcoming events through `CalendarContract`.
- `OpenMeteoWeatherRepository`: fetches current temperature and precipitation probability for the Today weather widget.
- `ScreenTimeRepository`: reads app usage through `UsageStatsManager`.
- `InstalledAppsRepository`: lists launchable apps through `PackageManager`.

Keep business rules in small testable classes where possible. If behavior can be verified without Android UI, prefer a local JVM test under `app/src/test`.

## Permissions And Privacy

The app currently declares:

- `READ_CALENDAR`: optional, used to show selected upcoming calendar events.
- `ACCESS_COARSE_LOCATION`: optional, requested only for the Today weather widget.
- `INTERNET`: used only by the Today weather widget to request current weather from Open-Meteo.
- `BIND_NOTIFICATION_LISTENER_SERVICE`: required by Android for the optional notification listener service. The user grants notification access through Android settings when adding the Today notification widget.
- `PACKAGE_USAGE_STATS`: optional special access, used for screen-time summaries and app budget prompts.
- `REQUEST_DELETE_PACKAGES`: used only after the user chooses an uninstall action.
- `BIND_ACCESSIBILITY_SERVICE`: required by Android for the optional lock-screen accessibility service.

The manifest disables cleartext traffic and app backup. Do not add analytics, sync, backup, export behavior, or new network uses beyond weather without updating `PRIVACY.md`, `SECURITY.md` when relevant, and this guide.

Notification access should stay scoped to active notifications displayed by the Today widget. Do not persist notification content beyond the current in-memory feed without updating the privacy documentation and reviewing the change as privacy-sensitive.

## Public-Readiness Checklist

Before making the repository public or merging a release-facing change:

- Run `git status --short --branch` and confirm only intentional files are changed.
- Run `git ls-files --others --exclude-standard` and confirm there are no untracked files that should be committed or ignored.
- Search for obvious secrets:

```sh
rg -n --hidden -g '!/.git/**' -g '!**/build/**' -g '!**/.gradle/**' -g '!**/.idea/**' -g '!**/.kotlin/**' -e '(?i)(api[_-]?key|secret|password|token|client[_-]?secret|private[_-]?key|BEGIN .* PRIVATE KEY|google-services\.json|keystore)'
```

- Confirm `local.properties`, signing keys, `.env*`, APK/AAB outputs, and service-account files are not tracked.
- Review `AndroidManifest.xml` for new permissions, exported components, backup changes, and cleartext/network behavior.
- Run `./gradlew test`.
- Run `./gradlew assembleDebug` when build files, resources, manifest, or UI code changed.

## Release Notes

The app version lives in `app/build.gradle.kts`:

- `versionCode`: monotonically increasing integer for Android package updates.
- `versionName`: user-visible semantic version.

Release artifacts should not be committed. Keep signing material outside the repository and provide signing values through local, ignored files or CI secrets.

## Contribution Standards

- Keep changes focused and easy to review.
- Add or update tests for behavior changes.
- Prefer existing Kotlin/XML/ViewBinding patterns.
- Avoid new dependencies unless they clearly reduce complexity.
- Document user-visible behavior in `README.md`.
- Document privacy, permission, storage, backup, or network changes before merge.
