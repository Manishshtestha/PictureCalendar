package com.example.picturecalendar.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.picturecalendar.data.MemoryRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarApp() {
    val context = LocalContext.current
    val repository = remember { MemoryRepository(context.applicationContext) }
    var displayedMonth by remember { mutableStateOf(repository.today().firstOfMonth()) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var pendingDay by remember { mutableStateOf<Calendar?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (!saved) pendingDay?.let { repository.photoFor(it).delete() }
        pendingDay = null
        refreshToken++
    }
    val memories = remember(displayedMonth, refreshToken) { repository.memoriesFor(displayedMonth) }

    CalendarScreen(
        month = displayedMonth,
        monthTitle = repository.monthTitle(displayedMonth),
        memories = memories,
        onPreviousMonth = { displayedMonth = displayedMonth.shiftedMonth(-1) },
        onNextMonth = { displayedMonth = displayedMonth.shiftedMonth(1) },
        onTakePhoto = { day ->
            pendingDay = day
            takePicture.launch(repository.photoUriFor(day))
        },
        photoFor = repository::photoFor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreen(
    month: Calendar,
    monthTitle: String,
    memories: Set<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTakePhoto: (Calendar) -> Unit,
    photoFor: (Calendar) -> File,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Picture Calendar") },
            navigationIcon = { TextButton(onClick = onPreviousMonth) { Text("‹") } },
            actions = { TextButton(onClick = onNextMonth) { Text("›") } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(monthTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            WeekdayLabels()
            MonthGrid(month, memories, onTakePhoto, photoFor)
        }
    }
}

@Composable
private fun WeekdayLabels() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    LazyVerticalGrid(columns = GridCells.Fixed(7), userScrollEnabled = false, modifier = Modifier.fillMaxWidth()) {
        items(days) { day ->
            Text(day, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun MonthGrid(month: Calendar, memories: Set<String>, onTakePhoto: (Calendar) -> Unit, photoFor: (Calendar) -> File) {
    val cells = remember(month) { month.calendarCells() }
    LazyVerticalGrid(columns = GridCells.Fixed(7), contentPadding = PaddingValues(4.dp), modifier = Modifier.fillMaxSize()) {
        items(cells) { day ->
            if (day == null) Box(Modifier.aspectRatio(1f).padding(2.dp))
            else DayCell(day, memories.contains(day.key()), photoFor(day), onTakePhoto)
        }
    }
}

@Composable
private fun DayCell(day: Calendar, hasMemory: Boolean, imageFile: File, onTakePhoto: (Calendar) -> Unit) {
    val canCapture = day.isToday()
    Card(modifier = Modifier.aspectRatio(1f).padding(2.dp).clickable(enabled = canCapture) { onTakePhoto(day) }) {
        Box(Modifier.fillMaxSize()) {
            if (hasMemory) {
                val bitmap = remember(imageFile.lastModified()) { BitmapFactory.decodeFile(imageFile.path)?.asImageBitmap() }
                if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            Text("${day.get(Calendar.DAY_OF_MONTH)}", modifier = Modifier.align(Alignment.TopStart).padding(4.dp), style = MaterialTheme.typography.labelSmall)
            if (!hasMemory && day.isToday()) Text("+", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun Calendar.firstOfMonth() = (clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
private fun Calendar.shiftedMonth(amount: Int) = (clone() as Calendar).apply { add(Calendar.MONTH, amount); set(Calendar.DAY_OF_MONTH, 1) }
private fun Calendar.key() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(time)
private fun Calendar.isToday() = key() == Calendar.getInstance().key()
private fun Calendar.calendarCells(): List<Calendar?> {
    val first = firstOfMonth()
    val blanks = first.get(Calendar.DAY_OF_WEEK) - 1
    val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    return List(blanks) { null } + List(days) { index ->
        (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, index + 1) }
    }
}
