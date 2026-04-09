package com.example.weathertd.model

enum class MetricType(
    val label: String,
    val unit: String,
    val analyticsName: String,
    val maxExpectedDelta: Double,
) {
    Temperature(
        label = "Temperature",
        unit = "C",
        analyticsName = "temperature",
        maxExpectedDelta = 8.0,
    ),
    FeelsLike(
        label = "Feels Like",
        unit = "C",
        analyticsName = "feels_like",
        maxExpectedDelta = 8.0,
    ),
    WindSpeed(
        label = "Wind Speed",
        unit = "m/s",
        analyticsName = "wind_speed",
        maxExpectedDelta = 5.0,
    ),
    FineDust(
        label = "PM10",
        unit = "ug/m3",
        analyticsName = "pm10",
        maxExpectedDelta = 30.0,
    ),
}

data class MetricComparison(
    val type: MetricType,
    val currentValue: Double,
    val previousValue: Double?,
) {
    val deltaValue: Double?
        get() = previousValue?.let { currentValue - it }
}
