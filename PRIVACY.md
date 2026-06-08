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
- Selected calendar IDs.

This data is stored with Android shared preferences.

## Optional Permission Data

Text Launcher can request:

- Calendar access, to show upcoming events from selected calendars.
- Usage access, to show screen-time summaries and compare app usage against daily budgets.
- Accessibility access, only to lock the screen when the configured lock-screen gesture is used.

These permissions are optional for their related features. The app should not transmit calendar events, usage stats, notes, or shortcut data to any server.

## Package Management

Text Launcher can ask Android to uninstall an app when you choose Uninstall from an app context menu. Android controls the confirmation flow. Text Launcher may receive the uninstall status and package removal broadcast so it can clean up local shortcuts for apps that are removed.

## Network

The current app does not include a network permission and is not designed to collect analytics.

## Backups

The Android manifest currently allows system backup. Depending on device settings, Android may include app-local data in system backups. Contributors should consider privacy implications before changing storage, backup, permissions, or data export behavior.
