package com.example.weathertd.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weathertd.data.location.hasLocationPermission
import com.example.weathertd.model.MainUiState
import com.example.weathertd.model.MetricComparison
import com.example.weathertd.ui.components.MetricCard
import com.example.weathertd.ui.theme.BackgroundBottom
import com.example.weathertd.ui.theme.BackgroundTop
import java.util.Locale

@Composable
fun MainScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onMetricClick: (MetricComparison) -> Unit,
    onDismissDetail: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onRefresh()
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BackgroundTop, BackgroundBottom),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderCard(
                    uiState = uiState,
                    onRefresh = {
                        requestLocationAndRefresh(
                            context = context,
                            onRefresh = onRefresh,
                            requestPermissions = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                        )
                    },
                )

                uiState.errorMessage?.let { message ->
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(onClick = onRefresh) {
                                Text("Retry")
                            }
                        }
                    }
                }

                uiState.metrics.forEach { metric ->
                    MetricCard(
                        metric = metric,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onMetricClick(metric) },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    uiState.selectedMetric?.let { metric ->
        MetricDetailDialog(
            metric = metric,
            onDismiss = onDismissDetail,
        )
    }
}

@Composable
private fun HeaderCard(
    uiState: MainUiState,
    onRefresh: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (uiState.locationTitle.isBlank()) "Today vs Yesterday" else uiState.locationTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = uiState.locationSubtitle.ifBlank { "Compare hourly weather from the nearest major point" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Chip(text = uiState.baselineMessage.ifBlank { "Waiting for yesterday cache" })
                Chip(text = uiState.updatedAtMessage.ifBlank { "Not updated yet" })
                Button(
                    onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text("Refresh with GPS")
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun MetricDetailDialog(
    metric: MetricComparison,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(metric.type.label)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Today: ${formatMetric(metric.currentValue, metric.type.unit)}")
                Text(
                    "Yesterday: ${
                        metric.previousValue?.let { formatMetric(it, metric.type.unit) } ?: "No data"
                    }",
                )
                Text(
                    "Delta: ${
                        metric.deltaValue?.let { formatSignedMetric(it, metric.type.unit) } ?: "N/A"
                    }",
                )
            }
        },
    )
}

private fun requestLocationAndRefresh(
    context: Context,
    onRefresh: () -> Unit,
    requestPermissions: () -> Unit,
) {
    if (context.hasLocationPermission()) {
        onRefresh()
    } else {
        requestPermissions()
    }
}

private fun formatMetric(value: Double, unit: String): String {
    return String.format(Locale.US, "%.1f %s", value, unit)
}

private fun formatSignedMetric(value: Double, unit: String): String {
    return String.format(Locale.US, "%+.1f %s", value, unit)
}
