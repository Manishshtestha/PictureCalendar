package com.example.picturecalendar.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Photo file names are their dates, avoiding a database in the first version. */
class MemoryRepository(private val context: Context) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val photoDirectory = File(context.filesDir, "daily_memories").apply { mkdirs() }

    fun today(): Calendar = Calendar.getInstance()
    fun dayKey(day: Calendar): String = dateFormat.format(day.time)
    fun monthTitle(month: Calendar): String =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    fun photoFor(day: Calendar): File = File(photoDirectory, "${dayKey(day)}.jpg")
    fun photoUriFor(day: Calendar): Uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", photoFor(day)
    )
    fun memoriesFor(month: Calendar): Set<String> {
        val prefix = monthFormat.format(month.time)
        return photoDirectory.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith(prefix) && it.extension == "jpg" }
            .map { it.nameWithoutExtension }.toSet()
    }
}
