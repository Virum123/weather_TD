package com.example.weathertd

import android.app.Application
import com.example.weathertd.analytics.AnalyticsLogger
import com.example.weathertd.analytics.FirebaseAnalyticsLogger
import com.example.weathertd.data.MajorPointLocator
import com.example.weathertd.data.WeatherRepository
import com.example.weathertd.data.local.WeatherCache
import com.example.weathertd.data.location.DeviceLocationRepository
import com.example.weathertd.data.network.OpenWeatherService

class WeatherTdApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val analyticsLogger = FirebaseAnalyticsLogger(this)
        appContainer = AppContainer(
            weatherRepository = WeatherRepository(
                service = OpenWeatherService.create(),
                cache = WeatherCache(this),
                locator = MajorPointLocator(),
                locationRepository = DeviceLocationRepository(this),
            ),
            analyticsLogger = analyticsLogger,
        )

        analyticsLogger.logAppOpen()
    }
}

data class AppContainer(
    val weatherRepository: WeatherRepository,
    val analyticsLogger: AnalyticsLogger,
)
