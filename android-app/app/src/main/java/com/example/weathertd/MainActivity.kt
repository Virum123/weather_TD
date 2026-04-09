package com.example.weathertd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.weathertd.ui.WeatherTdApp
import com.example.weathertd.ui.theme.WeatherTDTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as WeatherTdApplication).appContainer
        setContent {
            WeatherTDTheme {
                WeatherTdApp(container = container)
            }
        }
    }
}
