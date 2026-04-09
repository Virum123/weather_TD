package com.example.weathertd.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathertd.analytics.AnalyticsLogger
import com.example.weathertd.data.LocationResolutionSource
import com.example.weathertd.data.WeatherRepository
import com.example.weathertd.model.MainUiState
import com.example.weathertd.model.MetricComparison
import com.example.weathertd.model.MetricType
import com.example.weathertd.model.WeatherSnapshot
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val weatherRepository: WeatherRepository,
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                weatherRepository.loadComparison()
            }.onSuccess { result ->
                _uiState.value = MainUiState(
                    isLoading = false,
                    pointName = result.current.locationName,
                    locationTitle = "${result.current.locationName} vs Yesterday",
                    locationSubtitle = when (result.resolvedLocation.source) {
                        LocationResolutionSource.DeviceNearestPoint ->
                            "GPS mapped to the nearest major point"

                        LocationResolutionSource.FallbackPoint ->
                            "Location unavailable, using fallback major point"
                    },
                    baselineMessage = result.previous?.let { snapshot ->
                        "Compared with saved data from ${formatSnapshotTime(snapshot)}"
                    } ?: "No saved data from yesterday yet",
                    updatedAtMessage = "Updated ${formatSnapshotTime(result.current)}",
                    metrics = buildMetrics(
                        current = result.current,
                        previous = result.previous,
                    ),
                    selectedMetric = null,
                )

                analyticsLogger.logViewMain(
                    pointName = result.current.locationName,
                    sourceName = result.resolvedLocation.source.name,
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to load weather data",
                    )
                }
            }
        }
    }

    fun onMetricClick(metric: MetricComparison) {
        analyticsLogger.logClickDetail(metric.type.analyticsName)
        _uiState.update { state -> state.copy(selectedMetric = metric) }
    }

    fun dismissDetail() {
        _uiState.update { state -> state.copy(selectedMetric = null) }
    }

    private fun buildMetrics(
        current: WeatherSnapshot,
        previous: WeatherSnapshot?,
    ): List<MetricComparison> {
        return listOf(
            MetricComparison(
                type = MetricType.Temperature,
                currentValue = current.temperatureC,
                previousValue = previous?.temperatureC,
            ),
            MetricComparison(
                type = MetricType.FeelsLike,
                currentValue = current.feelsLikeC,
                previousValue = previous?.feelsLikeC,
            ),
            MetricComparison(
                type = MetricType.WindSpeed,
                currentValue = current.windSpeedMs,
                previousValue = previous?.windSpeedMs,
            ),
            MetricComparison(
                type = MetricType.FineDust,
                currentValue = current.pm10,
                previousValue = previous?.pm10,
            ),
        )
    }

    private fun formatSnapshotTime(snapshot: WeatherSnapshot): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.US)
        return snapshot.observedAt().format(formatter)
    }
}
