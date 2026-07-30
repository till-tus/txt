# Screenshot and Play Store Asset Map

This directory contains a consistent portrait screenshot set captured from Text Launcher 0.8.0 on the `ClearFrame_API_36` emulator.

All visible notes, calendar entries, shortcuts, usage data, and app choices are fictional or stock-emulator content. The captures use the same 1080×1920 canvas, wallpaper, font scale, portrait orientation, 10:02 status-bar time, and system-bar treatment.

## Directory structure

- `source/`: clean, uncropped app captures with no marketing overlay.
- `presentation/`: upload-ready 1080×1920, 24-bit RGB PNG exports.
- `template/`: editable SVG layouts for each export plus `presentation-template.svg` for future screens or localized captions.

To edit an export, update its SVG caption or linked source image in `template/`, then render the SVG at 1080×1920.

## Asset mapping

| Order | Source capture | Presentation export | Intended placement | Alt text |
| --- | --- | --- | --- | --- |
| 1 | `source/01-home.png` | `presentation/01-home.png` | README lead / Play Store opener | Text-first launcher Home with an analog clock, four shortcuts, and two Quick Access buttons. |
| 2 | `source/02-app-search.png` | `presentation/02-app-search.png` | README / Play Store search story | App search ranking Calendar, Camera, Contacts, and Voice Search for the query “ca”. |
| 3 | `source/03-notes.png` | `presentation/03-notes.png` | README / Play Store notes story | Notes page with a pinned project focus and three fictional reminders. |
| 4 | `source/04-calendar.png` | `presentation/04-calendar.png` | README / optional Play Store schedule story | Calendar page with four fictional events across two days. |
| 5 | `source/05-today.png` | `presentation/05-today.png` | README / Play Store glanceable-info story | Today page with next-event, weather, and pinned-note widgets. |
| 6 | `source/06-screen-time.png` | `presentation/06-screen-time.png` | README / Play Store awareness story | Screen Time page with a top-app recap, today total, average comparison, and weekly graph. |
| 7 | `source/07-app-blocking.png` | `presentation/07-app-blocking.png` | Play Store intentional-use closer | Chrome block-list prompt asking the user to choose an intended session length. |

The README uses the six clean core-screen captures. The Play Store narrative uses all seven presentation exports in the order above.

## Current Google Play fit

Checked on 2026-07-30 against Google’s [preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en):

- Google Play accepts up to eight phone screenshots.
- Screenshot files must be JPEG or 24-bit PNG without alpha.
- Each side must be between 320 and 3840 pixels, and the long side cannot exceed twice the short side.
- Taglines should be concise and occupy no more than 20% of the image.
- Status bars should avoid notifications and show complete connectivity and battery states.
- Assets should avoid calls to action, ranking claims, store badges, and obsolete device imagery.

The seven presentation PNGs are 1080×1920, 8-bit RGB with no alpha channel. Their caption area is under 20% of the canvas, and the layouts use a simple rounded app surface rather than a branded device frame.
