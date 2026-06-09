# Privacy

Text Launcher is designed as a local-first Android launcher. It should help people understand and shape their phone use without sending personal data away from the device.

## Data Stored Locally

The app stores the following on the device:

- Launcher settings.
- App shortcuts.
- Blocked app selections.
- Daily app budgets.
- App-use intention entries.
- Quick notes.
- Pinned-note state.
- Selected calendar IDs.
- Today widget layout and widget configuration, including notification-widget app filters.

This data is stored with Android shared preferences.

## Optional Permission Data

Text Launcher can request:

- Calendar access, to show upcoming events from selected calendars.
- Notification access, to show active notifications in the Today page notification widget. This access is requested only when that widget is added.
- Usage access, to show screen-time summaries and compare app usage against daily budgets.
- Accessibility access, only to lock the screen when the configured lock-screen gesture is used.

These permissions are optional for their related features. The app should not transmit calendar events, notifications, usage stats, notes, or shortcut data to any server.

## Package Management

Text Launcher can ask Android to uninstall an app when you choose Uninstall from an app context menu. Android controls the confirmation flow. Text Launcher may receive the uninstall status and package removal broadcast so it can clean up local shortcuts for apps that are removed.

## Network

The current app does not include a network permission and is not designed to collect analytics.

## Backups

The Android manifest disables app backup. This keeps local notes, Today widget configuration, shortcut choices, selected calendar IDs, app budgets, blocked app selections, and intention data out of Android cloud backup and device-transfer backup flows.

Contributors should treat backup behavior as privacy-sensitive. Any change that enables backup, data export, sync, analytics, or network transmission should be documented here and reviewed before release.
