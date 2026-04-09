package com.example.weathertd.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weathertd.analytics.AnalyticsLogger
import com.example.weathertd.data.WeatherRepository

class MainViewModelFactory(
    private val weatherRepository: WeatherRepository,
    private val analyticsLogger: AnalyticsLogger,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java))
        return MainViewModel(
            weatherRepository = weatherRepository,
            analyticsLogger = analyticsLogger,
        ) as T
    }
}
