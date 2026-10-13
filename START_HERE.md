# Picture Calendar — start here

The first version is intentionally small: tap a date, take a picture with the
phone's camera app, and see it in that date's calendar cell.

## What each file does

- `MainActivity.kt` starts the Compose UI.
- `ui/CalendarApp.kt` contains the screen, monthly grid, and system-camera launch.
- `data/MemoryRepository.kt` saves photos; the filename (`YYYY-MM-DD.jpg`) is
  the record for the day.
- `res/xml/file_paths.xml` safely lets the camera write into the app's private
  photo directory.

Photos are private to the app, not in Gallery. That avoids permissions for the
starter version, but uninstalling the app deletes them.

## Work through it

1. Sync Gradle in Android Studio and run it on a real device.
2. Tap any date, choose a camera app, take a photo, then return to the app.
3. Tap the same date again: it replaces that date's file, enforcing one photo
   per day in this initial design.
4. Navigate away and back with the arrows to confirm the memory stays in the
   correct month.

## Recommended next steps

1. Restrict capture to today by disabling other date cells in `DayCell`.
2. Add a full-screen memory-detail page for saved photos.
3. Add Room only when you need captions, moods, favorites, or edit history.
4. Create thumbnails off the main thread before supporting many years of photos.
5. Add export/backup before relying on the app for irreplaceable memories.

## Why this starts with the system camera

`TakePicture` has the installed camera app capture directly to the file you
provide. It avoids camera permissions and a lot of CameraX code. Move to
CameraX later only for an in-app preview, flash controls, or custom camera UI.
