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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.picturecalendar.R
import com.example.picturecalendar.ui.presentation.components.Header
import com.example.picturecalendar.ui.theme.PictureCalendarTheme
import java.util.Calendar
import java.util.GregorianCalendar

data class SimpleDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int
) {
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

    // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    fun firstDayOfWeekOffset(): Int {
        val cal = GregorianCalendar(year, month - 1, 1)
        return cal.get(Calendar.DAY_OF_WEEK) - 1
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
    val photoBitmap: Bitmap? = null
)

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    val todayDate = remember { SimpleDate.today() }
    var currentYearMonth by remember { mutableStateOf(SimpleYearMonth.now()) }
    var selectedDate by remember { mutableStateOf<SimpleDate?>(todayDate) }

    // Map storing captured photos per date
    var dayPhotos by remember { mutableStateOf<Map<SimpleDate, Bitmap>>(emptyMap()) }
    var photoTargetDate by remember { mutableStateOf<SimpleDate?>(null) }

    // Camera launcher contract
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val target = photoTargetDate ?: SimpleDate.today()
            dayPhotos = dayPhotos + (target to bitmap)
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
                Text(
                    text = "<",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { currentYearMonth = currentYearMonth.minusMonths(1) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            },
            trailingBtn = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {
                            currentYearMonth = SimpleYearMonth.now()
                            selectedDate = SimpleDate.today()
                        },
                        label = { Text("Today") },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { currentYearMonth = currentYearMonth.plusMonths(1) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        )

        CalendarBody(
            yearMonth = currentYearMonth,
            selectedDate = selectedDate,
            dayPhotos = dayPhotos,
            onDateSelected = { date -> selectedDate = date },
            onAddPhotoClick = { date ->
                photoTargetDate = date
                cameraLauncher.launch(null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Floating Action Button to capture photo for today's date
        Button(
            onClick = {
                photoTargetDate = SimpleDate.today()
                cameraLauncher.launch(null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("📷 Capture Photo for Today")
        }
    }
}

@Composable
fun CalendarBody(
    modifier: Modifier = Modifier,
    yearMonth: SimpleYearMonth = SimpleYearMonth.now(),
    selectedDate: SimpleDate? = null,
    dayPhotos: Map<SimpleDate, Bitmap> = emptyMap(),
    onDateSelected: (SimpleDate) -> Unit = {},
    onAddPhotoClick: (SimpleDate) -> Unit = {}
) {
    val days = remember(yearMonth, dayPhotos) { generateCalendarDays(yearMonth, dayPhotos) }
    val daysOfWeek = remember {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    // Zoom and pan gestures state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

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

            // Zoomable calendar grid area
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
                            onAddPhotoClick = {
                                onAddPhotoClick(day.date)
                            }
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
    onAddPhotoClick: () -> Unit,
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

    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = day.isCurrentMonth, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Display captured photo if present for this day
            if (day.photoBitmap != null) {
                Image(
                    bitmap = day.photoBitmap.asImageBitmap(),
                    contentDescription = "Photo for ${day.date.day}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Scrim for readable day number over photo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
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
                    color = if (day.isToday || day.photoBitmap != null) Color.White else contentColor
                )
            }

            // Photo Placeholder / Add Photo action button
            if (day.isCurrentMonth && day.photoBitmap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 3.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (day.isToday) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (day.isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable(onClick = onAddPhotoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (day.isToday) "📷 Today" else "+",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = if (day.isToday) 10.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.isToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }
}

private fun generateCalendarDays(
    yearMonth: SimpleYearMonth,
    dayPhotos: Map<SimpleDate, Bitmap>
): List<CalendarDay> {
    val today = SimpleDate.today()
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = yearMonth.firstDayOfWeekOffset()

    val calendarDays = mutableListOf<CalendarDay>()

    val previousMonth = yearMonth.minusMonths(1)
    val previousMonthDays = previousMonth.lengthOfMonth()
    for (i in firstDayOffset - 1 downTo 0) {
        val date = SimpleDate(previousMonth.year, previousMonth.month, previousMonthDays - i)
        calendarDays.add(
            CalendarDay(
                date = date,
                isCurrentMonth = false,
                photoBitmap = dayPhotos[date]
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
                photoBitmap = dayPhotos[date]
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
                photoBitmap = dayPhotos[date]
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
