package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TestStatus
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticCard
import com.example.ui.components.DiagnosticTopAppBar
import com.example.ui.components.TestPassFailButtons
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiagnosticCardBorder
import com.example.ui.theme.DiagnosticDarkBg
import com.example.ui.theme.DiagnosticSurface
import com.example.ui.theme.DiagnosticSurfaceVariant
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BatteryHealthScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val battery by viewModel.batteryState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val tempStatusColor = when {
        battery.temperatureCelsius > 44f -> RedDanger
        battery.temperatureCelsius > 38f -> AmberWarning
        else -> GreenSuccess
    }

    val batteryColor = when {
        battery.isCharging -> CyanAccent
        battery.levelPercent > 40 -> GreenSuccess
        battery.levelPercent > 20 -> AmberWarning
        else -> RedDanger
    }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Battery Diagnostics",
                onBack = onBack
            )
        },
        containerColor = DiagnosticDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Battery Circular Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
                    .padding(8.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(160.dp),
                    color = DiagnosticSurfaceVariant,
                    strokeWidth = 12.dp
                )
                CircularProgressIndicator(
                    progress = { battery.levelPercent / 100f },
                    modifier = Modifier.size(160.dp),
                    color = batteryColor,
                    strokeWidth = 12.dp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (battery.isCharging) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Charging",
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${battery.levelPercent}%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = battery.powerStatus,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (battery.isCharging) CyanAccent else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Status Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BatteryMetricTile(
                    title = "Temperature",
                    value = "${battery.temperatureCelsius}°C",
                    subtext = if (battery.temperatureCelsius < 38f) "Normal" else "Elevated",
                    icon = Icons.Default.Thermostat,
                    accentColor = tempStatusColor,
                    modifier = Modifier.weight(1f)
                )
                BatteryMetricTile(
                    title = "Voltage",
                    value = "${battery.voltageMv} mV",
                    subtext = "Stable Output",
                    icon = Icons.Default.ElectricBolt,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BatteryMetricTile(
                    title = "Cell Health",
                    value = battery.health,
                    subtext = "Factory Spec",
                    icon = Icons.Default.HealthAndSafety,
                    accentColor = if (battery.health.equals("Good", true)) GreenSuccess else AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                BatteryMetricTile(
                    title = "Power Source",
                    value = battery.plugType,
                    subtext = battery.technology,
                    icon = Icons.Default.Power,
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic Analysis Card
            DiagnosticCard(
                title = "Hardware Evaluation",
                subtitle = "Battery chemistry and charging circuitry"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EvaluationRow(
                        label = "Thermal Management",
                        status = if (battery.temperatureCelsius <= 40f) "Optimal (< 40°C)" else "Hot (> 40°C)",
                        isGood = battery.temperatureCelsius <= 40f
                    )
                    EvaluationRow(
                        label = "Cell Chemical State",
                        status = battery.health,
                        isGood = battery.health.equals("Good", true) || battery.health.equals("Normal", true)
                    )
                    EvaluationRow(
                        label = "Voltage Output",
                        status = "${battery.voltageMv} mV (${if (battery.voltageMv in 3500..4450) "Within Tolerance" else "Irregular"})",
                        isGood = battery.voltageMv in 3500..4450
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Record Diagnostic Result:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult(
                            "battery_health",
                            TestStatus.PASSED,
                            "${battery.health} condition, ${battery.temperatureCelsius}°C, ${battery.voltageMv}mV"
                        )
                        viewModel.updateTestResult(
                            "battery_temp",
                            if (battery.temperatureCelsius <= 40f) TestStatus.PASSED else TestStatus.WARNING,
                            "${battery.temperatureCelsius}°C"
                        )
                        viewModel.updateTestResult(
                            "battery_voltage",
                            TestStatus.PASSED,
                            "${battery.voltageMv} mV"
                        )
                        onBack()
                    },
                    onFail = {
                        viewModel.updateTestResult(
                            "battery_health",
                            TestStatus.FAILED,
                            "Degraded/Faulty: ${battery.health}, Temp: ${battery.temperatureCelsius}°C"
                        )
                        onBack()
                    },
                    onSkip = { onBack() }
                )
            }
        }
    }
}

@Composable
fun BatteryMetricTile(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DiagnosticSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, DiagnosticCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun EvaluationRow(
    label: String,
    status: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isGood) GreenSuccess else AmberWarning
        )
    }
}
