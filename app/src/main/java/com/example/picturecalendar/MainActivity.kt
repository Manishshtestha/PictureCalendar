package com.example.picturecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.picturecalendar.ui.CalendarApp
import com.example.picturecalendar.ui.theme.PictureCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PictureCalendarTheme {
                CalendarApp()
            }
        }
    }
}

