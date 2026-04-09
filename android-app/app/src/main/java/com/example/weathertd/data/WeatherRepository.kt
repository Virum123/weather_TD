package com.example.weathertd.data

import com.example.weathertd.BuildConfig
import com.example.weathertd.data.local.WeatherCache
import com.example.weathertd.data.location.DeviceLocationRepository
import com.example.weathertd.data.location.LocationLookupResult
import com.example.weathertd.data.network.AirPollutionResponse
import com.example.weathertd.data.network.HourlyWeatherDto
import com.example.weathertd.data.network.OneCallResponse
import com.example.weathertd.data.network.OpenWeatherService
import com.example.weathertd.model.WeatherSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

class WeatherRepository(
    private val service: OpenWeatherService,
    private val cache: WeatherCache,
    private val locator: MajorPointLocator,
    private val locationRepository: DeviceLocationRepository,
) {

    suspend fun loadComparison(): WeatherComparisonResult = coroutineScope {
        val apiKey = BuildConfig.OPEN_WEATHER_API_KEY
        check(apiKey.isNotBlank()) {
            "OPENWEATHER_API_KEY is empty. Add it to local.properties or the environment."
        }

        val locationLookup = locationRepository.lookupCurrentLocation()
        val anchorPoint = when (locationLookup) {
            is LocationLookupResult.Success -> locator.findNearest(
                latitude = locationLookup.latitude,
                longitude = locationLookup.longitude,
            )

            LocationLookupResult.PermissionMissing,
            LocationLookupResult.Unavailable -> MajorPointCatalog.fallback
        }

        val weatherDeferred = async {
            service.getHourlyWeather(
                latitude = anchorPoint.latitude,
                longitude = anchorPoint.longitude,
                apiKey = apiKey,
            )
        }
        val airDeferred = async {
            service.getAirPollution(
                latitude = anchorPoint.latitude,
                longitude = anchorPoint.longitude,
                apiKey = apiKey,
            )
        }

        val weatherResponse = weatherDeferred.await()
        val airResponse = airDeferred.await()

        val currentHourly = weatherResponse.closestHourly()
        val currentAir = airResponse.closestAirEntry(currentHourly.dt)
        val currentSnapshot = WeatherSnapshot(
            locationId = anchorPoint.id,
            locationName = anchorPoint.name,
            latitude = anchorPoint.latitude,
            longitude = anchorPoint.longitude,
            observedAtEpochSeconds = currentHourly.dt,
            timezoneOffsetSeconds = weatherResponse.timezoneOffset,
            temperatureC = currentHourly.temp,
            feelsLikeC = currentHourly.feelsLike,
            windSpeedMs = currentHourly.windSpeed,
            pm10 = currentAir.components.pm10,
        )

        val previousSnapshot = cache.findYesterdaySnapshot(
            locationId = currentSnapshot.locationId,
            observedAtEpochSeconds = currentSnapshot.observedAtEpochSeconds,
        )
        cache.saveSnapshot(currentSnapshot)

        WeatherComparisonResult(
            current = currentSnapshot,
            previous = previousSnapshot,
            resolvedLocation = ResolvedLocation(
                majorPoint = anchorPoint,
                source = when (locationLookup) {
                    is LocationLookupResult.Success -> LocationResolutionSource.DeviceNearestPoint
                    LocationLookupResult.PermissionMissing,
                    LocationLookupResult.Unavailable -> LocationResolutionSource.FallbackPoint
                },
                deviceLatitude = (locationLookup as? LocationLookupResult.Success)?.latitude,
                deviceLongitude = (locationLookup as? LocationLookupResult.Success)?.longitude,
            ),
        )
    }

    private fun OneCallResponse.closestHourly(): HourlyWeatherDto {
        return hourly.minByOrNull { entry -> abs(entry.dt - current.dt) }
            ?: error("No hourly weather data returned from OpenWeather.")
    }

    private fun AirPollutionResponse.closestAirEntry(targetEpochSeconds: Long) =
        list.minByOrNull { entry -> abs(entry.dt - targetEpochSeconds) }
            ?: error("No air pollution data returned from OpenWeather.")
}

data class WeatherComparisonResult(
    val current: WeatherSnapshot,
    val previous: WeatherSnapshot?,
    val resolvedLocation: ResolvedLocation,
)

data class ResolvedLocation(
    val majorPoint: MajorPoint,
    val source: LocationResolutionSource,
    val deviceLatitude: Double?,
    val deviceLongitude: Double?,
)

enum class LocationResolutionSource {
    DeviceNearestPoint,
    FallbackPoint,
}
