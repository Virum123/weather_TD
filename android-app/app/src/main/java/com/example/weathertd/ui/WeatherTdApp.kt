package com.example.weathertd.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weathertd.AppContainer
import com.example.weathertd.analytics.AnalyticsLogger

@Composable
fun WeatherTdApp(container: AppContainer) {
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(
            weatherRepository = container.weatherRepository,
            analyticsLogger = container.analyticsLogger,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrackStayTime(
        analyticsLogger = container.analyticsLogger,
        pointName = uiState.pointName,
    )

    MainScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onMetricClick = viewModel::onMetricClick,
        onDismissDetail = viewModel::dismissDetail,
    )
}

@Composable
private fun TrackStayTime(
    analyticsLogger: AnalyticsLogger,
    pointName: String,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestPointName by rememberUpdatedState(pointName)
    var startedAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    DisposableEffect(lifecycleOwner, analyticsLogger) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    startedAt = SystemClock.elapsedRealtime()
                }

                Lifecycle.Event.ON_STOP -> {
                    val elapsedSeconds = (SystemClock.elapsedRealtime() - startedAt) / 1_000L
                    if (elapsedSeconds > 0L) {
                        analyticsLogger.logStayTime(
                            seconds = elapsedSeconds,
                            pointName = latestPointName.ifBlank { null },
                        )
                    }
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
