package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun SensorsTestScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val sensors by viewModel.sensorReadings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Hardware Sensors",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Accelerometer & Spirit Level
            DiagnosticCard(
                title = "Accelerometer / 3D Tilt",
                subtitle = if (sensors.hasAccelerometer) "Hardware detected & active" else "Sensor not available"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SensorAxisRow("X-Axis (Pitch):", String.format("%.2f m/s²", sensors.accelX))
                        SensorAxisRow("Y-Axis (Roll):", String.format("%.2f m/s²", sensors.accelY))
                        SensorAxisRow("Z-Axis (Gravity):", String.format("%.2f m/s²", sensors.accelZ))

                        val isLevel = abs(sensors.accelX) < 0.5f && abs(sensors.accelY) < 0.5f
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isLevel) "Level: Balanced" else "Level: Tilted",
                            color = if (isLevel) GreenSuccess else AmberWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2D Spirit Level Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF090E1B))
                            .border(2.dp, CyanAccent, CircleShape)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF1E2B47),
                                radius = 25f,
                                style = Stroke(width = 2f)
                            )
                            val clampX = (sensors.accelX / 9.8f).coerceIn(-1f, 1f)
                            val clampY = (sensors.accelY / 9.8f).coerceIn(-1f, 1f)
                            val bubbleX = center.x - (clampX * 35f)
                            val bubbleY = center.y + (clampY * 35f)
                            val isFlat = abs(sensors.accelX) < 0.8f && abs(sensors.accelY) < 0.8f

                            drawCircle(
                                color = if (isFlat) GreenSuccess else CyanAccent,
                                radius = 12f,
                                center = Offset(bubbleX, bubbleY)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult(
                            "sensor_accel",
                            TestStatus.PASSED,
                            "Accelerometer active: X=${sensors.accelX.toInt()} Y=${sensors.accelY.toInt()} Z=${sensors.accelZ.toInt()} m/s²"
                        )
                    },
                    onFail = {
                        viewModel.updateTestResult("sensor_accel", TestStatus.FAILED, "Accelerometer unresponsive")
                    },
                    onSkip = { viewModel.updateTestResult("sensor_accel", TestStatus.SKIPPED, "Skipped") }
                )
            }

            // Compass / Magnetometer
            DiagnosticCard(
                title = "Digital Compass (Magnetometer)",
                subtitle = if (sensors.hasCompass) "Earth magnetic field vector" else "Compass sensor not available"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val heading = sensors.compassDegrees.toInt()
                        val cardinal = when (heading) {
                            in 338..360, in 0..22 -> "North (N)"
                            in 23..67 -> "North-East (NE)"
                            in 68..112 -> "East (E)"
                            in 113..157 -> "South-East (SE)"
                            in 158..202 -> "South (S)"
                            in 203..247 -> "South-West (SW)"
                            in 248..292 -> "West (W)"
                            else -> "North-West (NW)"
                        }
                        Text(
                            text = "$heading° $cardinal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Magnetic Azimuth heading",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF090E1B))
                            .border(1.dp, DiagnosticCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Compass Needle",
                            tint = RedDanger,
                            modifier = Modifier
                                .size(44.dp)
                                .rotate(-sensors.compassDegrees)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult(
                            "sensor_compass",
                            TestStatus.PASSED,
                            "${sensors.compassDegrees.toInt()}° heading verified"
                        )
                    },
                    onFail = {
                        viewModel.updateTestResult("sensor_compass", TestStatus.FAILED, "Compass sensor failure")
                    },
                    onSkip = { viewModel.updateTestResult("sensor_compass", TestStatus.SKIPPED, "Skipped") }
                )
            }

            // Ambient Light & Proximity
            DiagnosticCard(
                title = "Ambient Light & Proximity",
                subtitle = "Optical and infrared distance detectors"
            ) {
                // Light Sensor Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Illuminance (Lux):", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${sensors.ambientLux.toInt()} Lux",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (sensors.ambientLux / 1000f).coerceIn(0.02f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFFD600),
                        trackColor = Color(0xFF141F36)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Proximity Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sensors.isProximityNear) GreenSuccess.copy(alpha = 0.15f) else Color(0xFF0C1322))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (sensors.isProximityNear) "Proximity: NEAR (Covered)" else "Proximity: FAR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sensors.isProximityNear) GreenSuccess else CyanAccent
                        )
                        Text(
                            text = "Cover earpiece with hand to test",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = if (sensors.isProximityNear) GreenSuccess else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult(
                            "sensor_light",
                            TestStatus.PASSED,
                            "${sensors.ambientLux.toInt()} Lux measured"
                        )
                        viewModel.updateTestResult(
                            "sensor_prox",
                            TestStatus.PASSED,
                            "Proximity detector responsive"
                        )
                    },
                    onFail = {
                        viewModel.updateTestResult("sensor_light", TestStatus.FAILED, "Sensor unresponsive")
                        viewModel.updateTestResult("sensor_prox", TestStatus.FAILED, "Proximity unresponsive")
                    },
                    onSkip = {
                        viewModel.updateTestResult("sensor_light", TestStatus.SKIPPED, "Skipped")
                        viewModel.updateTestResult("sensor_prox", TestStatus.SKIPPED, "Skipped")
                    }
                )
            }
        }
    }
}

@Composable
fun SensorAxisRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
