package com.example.weathertd.model

data class MainUiState(
    val isLoading: Boolean = true,
    val pointName: String = "",
    val locationTitle: String = "",
    val locationSubtitle: String = "",
    val baselineMessage: String = "",
    val updatedAtMessage: String = "",
    val metrics: List<MetricComparison> = emptyList(),
    val selectedMetric: MetricComparison? = null,
    val errorMessage: String? = null,
)
