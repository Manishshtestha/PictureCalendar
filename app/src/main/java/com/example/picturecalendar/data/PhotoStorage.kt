package com.example.picturecalendar.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.picturecalendar.ui.presentation.screens.SimpleDate
import java.io.File
import java.io.FileOutputStream

data class DayPhotoData(
    val date: SimpleDate,
    val bitmap: Bitmap,
    val caption: String = "",
)

object PhotoStorage {

    private fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getPhotoFile(context: Context, date: SimpleDate): File {
        return File(getPhotosDir(context), "photo_${date.year}_${date.month}_${date.day}.jpg")
    }

    private fun getCaptionFile(context: Context, date: SimpleDate): File {
        return File(getPhotosDir(context), "caption_${date.year}_${date.month}_${date.day}.txt")
    }

    fun savePhoto(context: Context, date: SimpleDate, bitmap: Bitmap, caption: String = "") {
        try {
            val photoFile = getPhotoFile(context, date)
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            saveCaption(context, date, caption)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveCaption(context: Context, date: SimpleDate, caption: String) {
        try {
            val captionFile = getCaptionFile(context, date)
            if (caption.isBlank()) {
                if (captionFile.exists()) captionFile.delete()
            } else {
                captionFile.writeText(caption)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAllPhotos(context: Context): Map<SimpleDate, DayPhotoData> {
        val result = mutableMapOf<SimpleDate, DayPhotoData>()
        val dir = getPhotosDir(context)
        val files = dir.listFiles() ?: return emptyMap()

        for (file in files) {
            if (file.name.startsWith("photo_") && file.name.endsWith(".jpg")) {
                try {
                    val parts = file.name.removePrefix("photo_").removeSuffix(".jpg").split("_")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        val date = SimpleDate(year, month, day)

                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            val captionFile = getCaptionFile(context, date)
                            val caption = if (captionFile.exists()) captionFile.readText() else ""
                            result[date] = DayPhotoData(date, bitmap, caption)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    fun deletePhoto(context: Context, date: SimpleDate) {
        try {
            val photoFile = getPhotoFile(context, date)
            if (photoFile.exists()) photoFile.delete()

            val captionFile = getCaptionFile(context, date)
            if (captionFile.exists()) captionFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sharePhoto(context: Context, date: SimpleDate, caption: String) {
        val photoFile = getPhotoFile(context, date)
        if (!photoFile.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile,
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (caption.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Memory")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
