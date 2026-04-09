package com.example.weathertd.model

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class WeatherSnapshot(
    val locationId: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val observedAtEpochSeconds: Long,
    val timezoneOffsetSeconds: Int,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val windSpeedMs: Double,
    val pm10: Double,
) {
    fun observedAt(): OffsetDateTime {
        return Instant.ofEpochSecond(observedAtEpochSeconds)
            .atOffset(ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds))
    }
}
