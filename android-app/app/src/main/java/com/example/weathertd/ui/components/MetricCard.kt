package com.example.weathertd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.weathertd.model.MetricComparison
import com.example.weathertd.model.MetricType
import com.example.weathertd.ui.theme.CoolBlue
import com.example.weathertd.ui.theme.CoralRed
import com.example.weathertd.ui.theme.GoodGreen
import java.util.Locale
import kotlin.math.abs

@Composable
fun MetricCard(
    metric: MetricComparison,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val visual = metric.visualStyle()

    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = metric.type.leadingIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = metric.type.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = formatValue(metric.currentValue, metric.type.unit),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Yesterday",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = metric.previousValue?.let {
                            formatValue(it, metric.type.unit)
                        } ?: "No data",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color,
                        modifier = Modifier.size(visual.iconSize),
                    )
                    Text(
                        text = metric.deltaValue?.let { delta ->
                            String.format(Locale.US, "%+.1f %s", delta, metric.type.unit)
                        } ?: "N/A",
                        style = MaterialTheme.typography.titleMedium,
                        color = visual.color,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = metric.visualSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class MetricVisual(
    val icon: ImageVector,
    val color: Color,
    val iconSize: Dp,
)

@Composable
private fun MetricComparison.visualStyle(): MetricVisual {
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant
    val delta = deltaValue ?: return MetricVisual(
        icon = Icons.Rounded.HorizontalRule,
        color = neutralColor,
        iconSize = 24.dp,
    )

    val scale = (abs(delta) / type.maxExpectedDelta).coerceIn(0.0, 1.0)
    val iconSize = (24 + (scale * 24)).dp

    return when (type) {
        MetricType.Temperature,
        MetricType.FeelsLike -> if (delta > 0) {
            MetricVisual(Icons.Rounded.ArrowUpward, CoralRed, iconSize)
        } else if (delta < 0) {
            MetricVisual(Icons.Rounded.ArrowDownward, CoolBlue, iconSize)
        } else {
            MetricVisual(Icons.Rounded.HorizontalRule, GoodGreen, 24.dp)
        }

        MetricType.WindSpeed,
        MetricType.FineDust -> if (delta > 0) {
            MetricVisual(Icons.Rounded.ArrowUpward, CoralRed, iconSize)
        } else if (delta < 0) {
            MetricVisual(Icons.Rounded.ArrowDownward, GoodGreen, iconSize)
        } else {
            MetricVisual(Icons.Rounded.HorizontalRule, GoodGreen, 24.dp)
        }
    }
}

private fun MetricType.leadingIcon(): ImageVector {
    return when (this) {
        MetricType.Temperature -> Icons.Rounded.DeviceThermostat
        MetricType.FeelsLike -> Icons.Rounded.Whatshot
        MetricType.WindSpeed -> Icons.Rounded.Air
        MetricType.FineDust -> Icons.Rounded.Grain
    }
}

private fun MetricComparison.visualSummary(): String {
    val delta = deltaValue ?: return "Yesterday comparison becomes available after one saved hourly snapshot."
    return when (type) {
        MetricType.Temperature ->
            if (delta > 0) "Warmer than yesterday at the same hour."
            else if (delta < 0) "Cooler than yesterday at the same hour."
            else "Temperature is unchanged from yesterday."

        MetricType.FeelsLike ->
            if (delta > 0) "Feels warmer than yesterday."
            else if (delta < 0) "Feels cooler than yesterday."
            else "Feels the same as yesterday."

        MetricType.WindSpeed ->
            if (delta > 0) "Wind is stronger than yesterday."
            else if (delta < 0) "Wind has eased since yesterday."
            else "Wind speed is unchanged from yesterday."

        MetricType.FineDust ->
            if (delta > 0) "Air quality is worse than yesterday."
            else if (delta < 0) "Air quality improved from yesterday."
            else "PM10 level is unchanged from yesterday."
    }
}

private fun formatValue(value: Double, unit: String): String {
    return String.format(Locale.US, "%.1f %s", value, unit)
}
