package com.example.weathertd.data.network

import com.google.gson.annotations.SerializedName

data class OneCallResponse(
    val current: CurrentWeatherDto,
    val hourly: List<HourlyWeatherDto>,
    @SerializedName("timezone_offset")
    val timezoneOffset: Int,
)

data class CurrentWeatherDto(
    val dt: Long,
)

data class HourlyWeatherDto(
    val dt: Long,
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("wind_speed")
    val windSpeed: Double,
)

data class AirPollutionResponse(
    val list: List<AirPollutionEntryDto>,
)

data class AirPollutionEntryDto(
    val dt: Long,
    val components: AirComponentsDto,
)

data class AirComponentsDto(
    val pm10: Double,
    @SerializedName("pm2_5")
    val pm25: Double,
)
