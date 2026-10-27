package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DiagnosticItem
import com.example.data.model.TestCategory
import com.example.data.model.TestStatus
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticTopAppBar
import com.example.ui.components.TestStatusBadge
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
fun FullScanScreen(
    viewModel: DiagnosticViewModel,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val isScanning by viewModel.isFullScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val currentPhase by viewModel.currentScanPhase.collectAsStateWithLifecycle()
    val currentItemName by viewModel.currentScanItemName.collectAsStateWithLifecycle()
    val testItems by viewModel.testItems.collectAsStateWithLifecycle()

    val passedCount = testItems.count { it.status == TestStatus.PASSED }
    val failedCount = testItems.count { it.status == TestStatus.FAILED }
    val warningCount = testItems.count { it.status == TestStatus.WARNING }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Comprehensive Scan",
                onBack = if (isScanning) null else onBack
            )
        },
        containerColor = DiagnosticDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Radar Scanner & Circular Telemetry
            RadarScannerVisual(
                isScanning = isScanning,
                progress = scanProgress
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status & Phase Card
            Surface(
                color = DiagnosticSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPhase,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )

                    if (currentItemName.isNotEmpty() && isScanning) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GreenSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Evaluating: $currentItemName",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("scan_progress_bar"),
                        color = CyanAccent,
                        trackColor = DiagnosticSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress counters strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScanMetricChip("PASSED", passedCount, GreenSuccess)
                        ScanMetricChip("WARNING", warningCount, AmberWarning)
                        ScanMetricChip("FAILED", failedCount, RedDanger)
                        ScanMetricChip("TOTAL", testItems.size, TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Buttons
            if (!isScanning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.startFullComprehensiveScan {
                                onFinish()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("start_full_scan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (scanProgress > 0f) Icons.Default.RestartAlt else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (scanProgress > 0f) "Run Scan Again" else "Start Full Scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (scanProgress >= 1f || passedCount > 0) {
                        Button(
                            onClick = onFinish,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("view_scan_report_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenSuccess,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Report", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Surface(
                    color = DiagnosticSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comprehensive diagnostic in progress...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Items List
            Text(
                text = "COMPONENT DIAGNOSTIC PIPELINE (${testItems.size} CHECKS)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(testItems, key = { it.id }) { item ->
                    ScanItemRow(item = item)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RadarScannerVisual(
    isScanning: Boolean,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(150.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 8.dp.toPx()

            // Concentric rings
            drawCircle(
                color = DiagnosticCardBorder,
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = DiagnosticCardBorder.copy(alpha = 0.5f),
                radius = radius * 0.66f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = DiagnosticCardBorder.copy(alpha = 0.3f),
                radius = radius * 0.33f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Crosshairs
            drawLine(
                color = DiagnosticCardBorder.copy(alpha = 0.4f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = DiagnosticCardBorder.copy(alpha = 0.4f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            // Outer progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        CyanAccent.copy(alpha = 0.2f),
                        CyanAccent,
                        GreenSuccess
                    )
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Radar beam sweep when scanning
            if (isScanning) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            CyanAccent.copy(alpha = 0.05f),
                            CyanAccent.copy(alpha = 0.35f)
                        )
                    ),
                    startAngle = sweepAngle - 60f,
                    sweepAngle = 60f,
                    useCenter = true
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Text(
                text = if (isScanning) "SCANNING" else if (progress >= 1f) "COMPLETE" else "STANDBY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isScanning) CyanAccent else if (progress >= 1f) GreenSuccess else TextMuted,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun ScanMetricChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )
    }
}

@Composable
fun ScanItemRow(item: DiagnosticItem) {
    val categoryIcon = when (item.category) {
        TestCategory.CONNECTIVITY -> Icons.Default.Wifi
        TestCategory.CAMERA -> Icons.Default.CameraAlt
        TestCategory.AUDIO -> Icons.AutoMirrored.Filled.VolumeUp
        TestCategory.SCREEN -> Icons.Default.TouchApp
        TestCategory.BATTERY -> Icons.Default.ElectricBolt
        TestCategory.SENSORS -> Icons.Default.Sensors
        TestCategory.HAPTICS_TORCH -> Icons.Default.Vibration
        TestCategory.SYSTEM_INFO -> Icons.Default.Assessment
    }

    Surface(
        color = DiagnosticSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DiagnosticSurfaceVariant)
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (item.detailMessage.isNotEmpty()) {
                    Text(
                        text = item.detailMessage,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            TestStatusBadge(status = item.status)
        }
    }
}
