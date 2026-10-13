# Build guide: Picture Calendar MVP

This guide assumes the blank Compose project currently in this repository.
Work through the sections in order and keep the app runnable after every
section.

## 1. Learn the starting project

Open `MainActivity.kt`. Android launches this class; `setContent` places
Compose UI on screen. The `PictureCalendarTheme` wrapper supplies colors and
typography. Replace the template greeting with a single `CalendarApp()`
composable only after creating that composable in `ui/CalendarApp.kt`.

Suggested folders:

```text
com.example.picturecalendar/
  data/        // file storage only
  ui/          // Compose screens and small UI helpers
  MainActivity.kt
```

Avoid ViewModels and navigation until the screen needs them. One screen is
enough for this MVP.

## 2. Build the calendar before adding photos

Keep a `Calendar` representing the first day of the visible month. From it,
derive these values:

```kotlin
val leadingBlankCells = firstDay.get(Calendar.DAY_OF_WEEK) - 1
val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
```

Render seven weekday labels and a Compose `LazyVerticalGrid` with seven fixed
columns. Add `leadingBlankCells` empty cells, then a cell for every day. Use
previous/next buttons to clone the `Calendar`, add or subtract one month, and
reset the day to `1`.

Before adding the camera, manually check February in leap and non-leap years,
plus a month beginning on Sunday and one beginning on Saturday.

## 3. Save one private photo per date

Create `MemoryRepository(context)`. It should create this directory lazily:

```kotlin
File(context.filesDir, "daily_memories").apply { mkdirs() }
```

Use a fixed, locale-independent format for filenames:

```kotlin
SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.time)
```

`photoFor(day)` should return `daily_memories/<date>.jpg`. This gives you the
one-photo rule naturally: taking another photo for that date overwrites the
same destination. To find memories for a month, list files whose names start
with that month's `yyyy-MM` prefix.

Use private app storage first. It needs no media permissions, but users should
eventually get an export or backup feature because uninstalling removes it.

## 4. Let the system camera write to that file

Android camera apps must never receive a raw file path. Add a `FileProvider`
to `AndroidManifest.xml` and a `res/xml/file_paths.xml` resource that exposes
only `filesDir/daily_memories`. Its authority should be
`${applicationId}.fileprovider`.

In Compose, register the contract once:

```kotlin
val takePicture = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicture(),
) { saved ->
    // reload calendar state when saved; delete the pending file when cancelled
}
```

When the user taps **today**, obtain a content `Uri` from
`FileProvider.getUriForFile(...)` and call `takePicture.launch(uri)`.

Keep the date being captured in state. If the contract returns `false`, delete
that date's pending file so a cancelled capture is not rendered as a memory.
Test this on a real device—an emulator may not have a useful camera app.

## 5. Display a photo efficiently

For the first version, decode each saved JPEG with `BitmapFactory.decodeFile`
and convert it with `asImageBitmap()`. Show it with Compose `Image` and
`ContentScale.Crop` inside the date cell. Use a neutral background for empty
cells and a visible day number over the image.

This is sufficient for a single month but not for years of full-resolution
images. When performance becomes noticeable, decode a scaled bitmap or save a
separate thumbnail; do not add that optimization before measuring the MVP.

## 6. Enforce the actual product rule

Decide whether retaking today's picture is allowed. The week plan permits
replacement. Whatever you choose:

- only today's cell launches the camera;
- future and previous cells are not clickable for capture;
- a date maps to one filename;
- a saved older photo is display-only until a memory-detail screen exists.

Do not use the visible month alone to decide "today". Compare each day with a
fresh device-local `Calendar.getInstance()` date.

## 7. Test it manually

On a physical device, verify this sequence:

1. Open the app on today's month.
2. Start and cancel capture; no photo should appear.
3. Capture a photo; it should show in today’s cell.
4. Restart the app; it should remain.
5. Move to another month and back; it should remain on the same date.
6. Retake the photo; exactly one cell should update.

Commit after each completed milestone. Keep commits small, with messages such
as `Add private photo repository` or `Show memories in month grid`.
