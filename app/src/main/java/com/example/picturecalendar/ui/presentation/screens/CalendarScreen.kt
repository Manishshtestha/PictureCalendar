package com.example.picturecalendar.ui.presentation.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.picturecalendar.R
import com.example.picturecalendar.data.DayPhotoData
import com.example.picturecalendar.data.PhotoStorage
import com.example.picturecalendar.ui.presentation.components.Header
import com.example.picturecalendar.ui.theme.PictureCalendarTheme
import java.util.Calendar
import java.util.GregorianCalendar

data class SimpleDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int
) {
    fun formattedString(): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val mName = monthNames[(month - 1).coerceIn(0, 11)]
        return "$mName $day, $year"
    }

    companion object {
        fun today(): SimpleDate {
            val cal = Calendar.getInstance()
            return SimpleDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}

data class SimpleYearMonth(
    val year: Int,
    val month: Int // 1..12
) {
    fun monthName(): String {
        val names = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return names[(month - 1).coerceIn(0, 11)]
    }

    fun plusMonths(count: Int): SimpleYearMonth {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1 + count)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return SimpleYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun minusMonths(count: Int): SimpleYearMonth = plusMonths(-count)

    fun lengthOfMonth(): Int {
        val cal = GregorianCalendar(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Offset based on week starting day (0-indexed)
    fun firstDayOfWeekOffset(startWeekOnMonday: Boolean): Int {
        val cal = GregorianCalendar(year, month - 1, 1)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2 ...
        return if (startWeekOnMonday) {
            (dayOfWeek + 5) % 7
        } else {
            dayOfWeek - 1
        }
    }

    companion object {
        fun now(): SimpleYearMonth {
            val cal = Calendar.getInstance()
            return SimpleYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}

/**
 * Data representation for a single day in the photo calendar grid.
 */
data class CalendarDay(
    val date: SimpleDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean = false,
    val photoData: DayPhotoData? = null
)

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayDate = remember { SimpleDate.today() }

    var currentYearMonth by remember { mutableStateOf(SimpleYearMonth.now()) }
    var selectedDate by remember { mutableStateOf<SimpleDate?>(todayDate) }

    // Week start setting: false = Sunday start, true = Monday start
    var startWeekOnMonday by remember { mutableStateOf(false) }

    // Persistent day photos
    var dayPhotos by remember { mutableStateOf<Map<SimpleDate, DayPhotoData>>(emptyMap()) }

    // Detail dialog state
    var activeDetailPhotoDate by remember { mutableStateOf<SimpleDate?>(null) }

    // Load persisted photos on start
    LaunchedEffect(Unit) {
        dayPhotos = PhotoStorage.loadAllPhotos(context)
    }

    // Camera launcher contract for capturing photos
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            // Only allow capture/retake for TODAY
            val existingCaption = dayPhotos[todayDate]?.caption ?: ""
            PhotoStorage.savePhoto(context, todayDate, bitmap, existingCaption)
            dayPhotos = PhotoStorage.loadAllPhotos(context)
        }
    }

    val monthTitle = remember(currentYearMonth) {
        "${currentYearMonth.monthName()} ${currentYearMonth.year}"
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Header(
            title = "Photo Calendar",
            subtitle = monthTitle,
            icon = painterResource(R.drawable.outline_dashboard_24),
            leadingBtn = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { currentYearMonth = currentYearMonth.minusMonths(1) }
                        .semantics { contentDescription = "Previous Month" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◀",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingBtn = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { currentYearMonth = currentYearMonth.plusMonths(1) }
                            .semantics { contentDescription = "Next Month" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        // Sub-header controls (Today Jump & Week Start Setting)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    currentYearMonth = SimpleYearMonth.now()
                    selectedDate = SimpleDate.today()
                },
                label = { Text("Jump to Today") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )

            FilterChip(
                selected = startWeekOnMonday,
                onClick = { startWeekOnMonday = !startWeekOnMonday },
                label = {
                    Text(if (startWeekOnMonday) "Week: Mon" else "Week: Sun")
                }
            )
        }

        // Main Calendar Grid
        CalendarBody(
            yearMonth = currentYearMonth,
            selectedDate = selectedDate,
            startWeekOnMonday = startWeekOnMonday,
            dayPhotos = dayPhotos,
            onDateSelected = { date ->
                selectedDate = date
                if (dayPhotos.containsKey(date)) {
                    activeDetailPhotoDate = date
                }
            },
            onCaptureTodayClick = {
                cameraLauncher.launch(null)
            },
            onSwipeNextMonth = {
                currentYearMonth = currentYearMonth.plusMonths(1)
            },
            onSwipePreviousMonth = {
                currentYearMonth = currentYearMonth.minusMonths(1)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Primary Action Button (Today Capture / Retake)
        val todayHasPhoto = dayPhotos.containsKey(todayDate)
        Button(
            onClick = {
                cameraLauncher.launch(null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (todayHasPhoto) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (todayHasPhoto) "📷 Retake Today's Photo" else "📷 Capture Today's Photo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Full-screen / Detail dialog for viewing, editing caption, sharing or deleting photo
    activeDetailPhotoDate?.let { date ->
        val photoData = dayPhotos[date]
        if (photoData != null) {
            PhotoDetailDialog(
                photoData = photoData,
                isToday = date == todayDate,
                onDismiss = { activeDetailPhotoDate = null },
                onSaveCaption = { newCaption ->
                    PhotoStorage.saveCaption(context, date, newCaption)
                    dayPhotos = PhotoStorage.loadAllPhotos(context)
                },
                onRetakePhoto = {
                    activeDetailPhotoDate = null
                    cameraLauncher.launch(null)
                },
                onDeletePhoto = {
                    PhotoStorage.deletePhoto(context, date)
                    dayPhotos = PhotoStorage.loadAllPhotos(context)
                    activeDetailPhotoDate = null
                },
                onSharePhoto = {
                    PhotoStorage.sharePhoto(context, date, photoData.caption)
                }
            )
        }
    }
}

@Composable
fun CalendarBody(
    modifier: Modifier = Modifier,
    yearMonth: SimpleYearMonth = SimpleYearMonth.now(),
    selectedDate: SimpleDate? = null,
    startWeekOnMonday: Boolean = false,
    dayPhotos: Map<SimpleDate, DayPhotoData> = emptyMap(),
    onDateSelected: (SimpleDate) -> Unit = {},
    onCaptureTodayClick: () -> Unit = {},
    onSwipeNextMonth: () -> Unit = {},
    onSwipePreviousMonth: () -> Unit = {}
) {
    val days = remember(yearMonth, dayPhotos, startWeekOnMonday) {
        generateCalendarDays(yearMonth, dayPhotos, startWeekOnMonday)
    }

    val daysOfWeek = remember(startWeekOnMonday) {
        if (startWeekOnMonday) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        } else {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        }
    }

    // Zoom and pan gestures state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Swipe horizontal drag detection for changing months when unzoomed
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        Column {
            // Weekday header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Zoomable and Swipeable calendar grid container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    .pointerInput(scale) {
                        if (scale == 1f) {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDragX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount
                                },
                                onDragEnd = {
                                    if (totalDragX < -120f) {
                                        onSwipeNextMonth()
                                    } else if (totalDragX > 120f) {
                                        onSwipePreviousMonth()
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            do {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                if (zoom != 1f || pan != Offset.Zero) {
                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = newScale

                                    if (newScale > 1f) {
                                        val maxX = (size.width * (newScale - 1f)) / 2f
                                        val maxY = (size.height * (newScale - 1f)) / 2f
                                        offset = Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                        )
                                    } else {
                                        offset = Offset.Zero
                                    }

                                    if (event.changes.size > 1 || scale > 1f) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = scale == 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(days) { day ->
                        CalendarDayCell(
                            day = day,
                            isSelected = day.date == selectedDate,
                            onClick = {
                                if (day.isCurrentMonth) {
                                    onDateSelected(day.date)
                                }
                            },
                            onCaptureTodayClick = onCaptureTodayClick
                        )
                    }
                }
            }
        }

        // Reset Zoom chip overlay if zoomed in
        if (scale > 1.05f) {
            AssistChip(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                label = { Text("Reset Zoom") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCaptureTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        !day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dateDescription = remember(day) {
        val hasPhoto = if (day.photoData != null) ", has photo" else ""
        val todayStr = if (day.isToday) ", Today" else ""
        "${day.date.formattedString()}$todayStr$hasPhoto"
    }

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = day.isCurrentMonth, onClick = onClick)
            .semantics { contentDescription = dateDescription },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Display photo if present for this day
            if (day.photoData != null) {
                Image(
                    bitmap = day.photoData.bitmap.asImageBitmap(),
                    contentDescription = "Photo for ${day.date.day}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark scrim for day number contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }

            // Day Number Badge
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart)
                    .then(
                        if (day.isToday) {
                            Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        } else Modifier
                    )
            ) {
                Text(
                    text = day.date.day.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday || day.photoData != null) Color.White else contentColor
                )
            }

            // Caption Indicator Badge if present
            if (day.photoData?.caption?.isNotBlank() == true) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "📝",
                        fontSize = 10.sp
                    )
                }
            }

            // Capture button ONLY for Today (when no photo exists yet)
            if (day.isCurrentMonth && day.isToday && day.photoData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 3.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable(onClick = onCaptureTodayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷 Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Full screen / Detail Dialog for viewing photo, editing caption, sharing, retaking, or deleting.
 */
@Composable
fun PhotoDetailDialog(
    photoData: DayPhotoData,
    isToday: Boolean,
    onDismiss: () -> Unit,
    onSaveCaption: (String) -> Unit,
    onRetakePhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    onSharePhoto: () -> Unit
) {
    var captionText by remember { mutableStateOf(photoData.caption) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Date & Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = photoData.date.formattedString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Full Photo View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = photoData.bitmap.asImageBitmap(),
                        contentDescription = "Full photo for ${photoData.date.formattedString()}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Caption Input
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Photo Caption (Optional)") },
                    placeholder = { Text("What made this day special?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Save Caption Button
                if (captionText != photoData.caption) {
                    Button(
                        onClick = { onSaveCaption(captionText) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Caption")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Row (Share, Retake, Delete)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSharePhoto,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📤 Share Memory")
                        }

                        if (isToday) {
                            Button(
                                onClick = onRetakePhoto,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📷 Retake")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑 Delete Photo")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Photo?") },
            text = { Text("Are you sure you want to delete this memory? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onDeletePhoto) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun generateCalendarDays(
    yearMonth: SimpleYearMonth,
    dayPhotos: Map<SimpleDate, DayPhotoData>,
    startWeekOnMonday: Boolean
): List<CalendarDay> {
    val today = SimpleDate.today()
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = yearMonth.firstDayOfWeekOffset(startWeekOnMonday)

    val calendarDays = mutableListOf<CalendarDay>()

    val previousMonth = yearMonth.minusMonths(1)
    val previousMonthDays = previousMonth.lengthOfMonth()
    for (i in firstDayOffset - 1 downTo 0) {
        val date = SimpleDate(previousMonth.year, previousMonth.month, previousMonthDays - i)
        calendarDays.add(
            CalendarDay(
                date = date,
                isCurrentMonth = false,
                photoData = dayPhotos[date]
            )
        )
    }

    for (day in 1..daysInMonth) {
        val date = SimpleDate(yearMonth.year, yearMonth.month, day)
        calendarDays.add(
            CalendarDay(
                date = date,
                isCurrentMonth = true,
                isToday = date == today,
                photoData = dayPhotos[date]
            )
        )
    }

    val remainingCells = (7 - (calendarDays.size % 7)) % 7
    val nextMonth = yearMonth.plusMonths(1)
    for (day in 1..remainingCells) {
        val date = SimpleDate(nextMonth.year, nextMonth.month, day)
        calendarDays.add(
            CalendarDay(
                date = date,
                isCurrentMonth = false,
                photoData = dayPhotos[date]
            )
        )
    }

    return calendarDays
}

@Preview(showBackground = true)
@Composable
fun CalendarScreenPreview() {
    PictureCalendarTheme {
        CalendarScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarBodyPreview() {
    PictureCalendarTheme {
        Surface {
            CalendarBody(
                yearMonth = SimpleYearMonth.now()
            )
        }
    }
}
