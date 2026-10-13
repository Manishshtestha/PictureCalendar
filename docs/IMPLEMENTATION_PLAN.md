# Picture Calendar: seven-day implementation plan

## Week-one outcome

An offline Android app where a person can take one photo for **today**, see it
in the correct day of a monthly calendar, reopen the app without losing it, and
navigate to older months. A second capture on the same day may replace the
first photo; it must not create a second memory for that date.

## Decisions for this week

- Build with Kotlin and Jetpack Compose (the project already has both).
- Start the installed camera app with `ActivityResultContracts.TakePicture`.
- Save JPEGs in the app's private `filesDir/daily_memories` folder.
- Name each file `yyyy-MM-dd.jpg`. The name is the one-memory-per-day rule.
- Do not add Room, cloud sync, shared Gallery access, login, captions, or
  notifications this week. Each is useful later, but none is required for the
  first reliable experience.

## Daily plan

| Day | Goal | Deliverable | Done when |
| --- | --- | --- | --- |
| 1 | Prepare the app | A simple Compose screen with an app bar and a placeholder calendar area | It launches on an emulator/device without errors. |
| 2 | Model dates and layout | A pure monthly grid with weekday headers, month title, and previous/next controls | February, 30-day, and 31-day months align correctly. |
| 3 | Take a photo | A `FileProvider` and `TakePicture` launcher that writes a test photo to the app's private storage | Tapping today's action opens a camera and returns successfully. |
| 4 | Enforce daily storage | A small `MemoryRepository` that maps a calendar date to `yyyy-MM-dd.jpg` | The same date maps to exactly one file; cancellation leaves no empty file. |
| 5 | Show memories | Saved photos display as cropped thumbnails in their date cells | Close/reopen the app and navigate away/back; the right cell still displays the photo. |
| 6 | Make it usable | Disable future/past capture, show helpful empty/error states, and check accessibility labels | A first-time user can understand what to tap and cannot accidentally add two dates. |
| 7 | Test and package | Manual test pass, focused unit tests for date/file helpers, README updates, release build | The acceptance checklist below passes on a physical Android device. |

## Suggested order inside each day

1. Make one small change.
2. Run the app immediately.
3. Commit a focused checkpoint, for example `Add month grid`.
4. Write down one thing that broke or was confusing before moving on.

This makes it easy to discard an experiment without losing the last working
version.

## Acceptance checklist

- The app starts on the current month and marks today clearly.
- Today is the only date that offers capture.
- A successful capture appears in today's cell after returning to the app.
- Retaking today's photo replaces the existing memory rather than creating a
  second entry.
- Cancelled capture does not create a visible memory.
- Month navigation does not move or hide a saved memory.
- An app restart preserves a saved memory.
- The layout remains usable in portrait on a real phone.

## Risks and containment

| Risk | Keep it contained this week |
| --- | --- |
| Camera apps behave differently | Test on a physical device early, on Day 3. |
| Large images make the grid slow | Decode a scaled thumbnail before expanding beyond one month. |
| Private photos disappear on uninstall | Document this limitation; add export/backup after the MVP. |
| Date changes around midnight/time zones | Use the device-local date consistently and test near a date boundary if possible. |

## After the week

The most valuable next features are a full-screen memory detail page, optional
captions, image export/backup, and then a Room database for metadata. Add
CameraX only if the system camera flow stops meeting the product needs.
