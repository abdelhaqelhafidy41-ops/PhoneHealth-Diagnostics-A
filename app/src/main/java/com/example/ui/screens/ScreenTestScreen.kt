package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestStatus
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticTopAppBar
import com.example.ui.components.TestPassFailButtons
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiagnosticDarkBg
import com.example.ui.theme.DiagnosticSurface
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScreenTestScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dead Pixels", "Multi-Touch", "Touch Grid")

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Screen Diagnostics",
                onBack = onBack
            )
        },
        containerColor = DiagnosticDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DiagnosticDarkBg,
                contentColor = CyanAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanAccent
                    )
                },
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) CyanAccent else TextSecondary
                            )
                        },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                                1 -> Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                else -> Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> DeadPixelTestView(
                    onRecordResult = { pass, msg ->
                        viewModel.updateTestResult(
                            "screen_dead_pixel",
                            if (pass) TestStatus.PASSED else TestStatus.FAILED,
                            msg
                        )
                        selectedTab = 1
                    }
                )
                1 -> MultiTouchTestView(
                    onRecordResult = { pass, msg ->
                        viewModel.updateTestResult(
                            "screen_multitouch",
                            if (pass) TestStatus.PASSED else TestStatus.FAILED,
                            msg
                        )
                        selectedTab = 2
                    }
                )
                2 -> TouchGridDigitizerView(
                    onRecordResult = { pass, msg ->
                        viewModel.updateTestResult(
                            "screen_digitizer",
                            if (pass) TestStatus.PASSED else TestStatus.FAILED,
                            msg
                        )
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
fun DeadPixelTestView(
    onRecordResult: (Boolean, String) -> Unit
) {
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.White,
        Color.Black,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta
    )
    val colorNames = listOf("Red", "Green", "Blue", "White", "Black", "Yellow", "Cyan", "Magenta")
    var colorIndex by remember { mutableIntStateOf(0) }
    var hudVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors[colorIndex])
            .clickable {
                colorIndex = (colorIndex + 1) % colors.size
            }
    ) {
        if (hudVisible) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dead Pixel Inspection",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Current: ${colorNames[colorIndex]} (${colorIndex + 1}/${colors.size}) • Tap anywhere to cycle",
                        color = CyanAccent,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Examine screen for stuck or unlit pixel dots:",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TestPassFailButtons(
                        onPass = { onRecordResult(true, "All ${colors.size} color fields clean, no dead pixels") },
                        onFail = { onRecordResult(false, "Dead/stuck pixel detected on ${colorNames[colorIndex]} field") },
                        onSkip = { onRecordResult(true, "Visual inspection skipped") }
                    )
                }
            }
        }
    }
}

@Composable
fun MultiTouchTestView(
    onRecordResult: (Boolean, String) -> Unit
) {
    val activePointers = remember { mutableStateMapOf<Long, Offset>() }
    var maxPointersSeen by remember { mutableIntStateOf(0) }

    val pointerColors = listOf(
        CyanAccent, GreenSuccess, RedDanger, Color(0xFFFFD600),
        Color(0xFFE040FB), Color(0xFF00B0FF), Color(0xFFFF6E40),
        Color(0xFF69F0AE), Color(0xFFFF4081), Color(0xFF7C4DFF)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Surface(
            color = DiagnosticSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Fingers: ${activePointers.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activePointers.size > 1) GreenSuccess else TextPrimary
                    )
                    Text(
                        text = "Max Detected: $maxPointersSeen fingers",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                OutlinedButton(
                    onClick = {
                        activePointers.clear()
                        maxPointersSeen = 0
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF060912), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes
                            activePointers.clear()
                            pointers.forEach { change ->
                                if (change.pressed) {
                                    activePointers[change.id.value] = change.position
                                }
                            }
                            if (activePointers.size > maxPointersSeen) {
                                maxPointersSeen = activePointers.size
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Crosshairs / touch points
                var index = 0
                activePointers.forEach { (_, position) ->
                    val color = pointerColors[index % pointerColors.size]
                    drawCircle(
                        color = color.copy(alpha = 0.25f),
                        radius = 80f,
                        center = position
                    )
                    drawCircle(
                        color = color,
                        radius = 40f,
                        center = position,
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = position
                    )
                    index++
                }
            }

            if (activePointers.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Place multiple fingers on screen",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Test 2, 5, or 10 simultaneous touches",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TestPassFailButtons(
            onPass = {
                val msg = if (maxPointersSeen >= 2) "$maxPointersSeen multi-touch points verified" else "Touch responsive"
                onRecordResult(true, msg)
            },
            onFail = { onRecordResult(false, "Failed to register multi-touch points") },
            onSkip = { onRecordResult(true, "Multi-touch skipped") }
        )
    }
}

@Composable
fun TouchGridDigitizerView(
    onRecordResult: (Boolean, String) -> Unit
) {
    val rows = 10
    val cols = 6
    val totalCells = rows * cols
    val touchedCells = remember { mutableStateListOf<Int>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight - 120.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Touch Grid Coverage",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${touchedCells.size} / $totalCells cells filled (${(touchedCells.size * 100 / totalCells)}%)",
                        fontSize = 13.sp,
                        color = if (touchedCells.size >= totalCells * 0.9) GreenSuccess else CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                OutlinedButton(
                    onClick = { touchedCells.clear() },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Clear", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(availableHeight)
                    .background(Color(0xFF080D1A), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val pos = change.position
                            val cellWidth = size.width / cols
                            val cellHeight = size.height / rows
                            val col = (pos.x / cellWidth).toInt().coerceIn(0, cols - 1)
                            val row = (pos.y / cellHeight).toInt().coerceIn(0, rows - 1)
                            val cellIndex = row * cols + col
                            if (!touchedCells.contains(cellIndex)) {
                                touchedCells.add(cellIndex)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellW = size.width / cols
                    val cellH = size.height / rows

                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val index = r * cols + c
                            val isTouched = touchedCells.contains(index)
                            val left = c * cellW
                            val top = r * cellH

                            drawRoundRect(
                                color = if (isTouched) GreenSuccess.copy(alpha = 0.85f) else Color(0xFF141F36),
                                topLeft = Offset(left + 2f, top + 2f),
                                size = Size(cellW - 4f, cellH - 4f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                if (touchedCells.isEmpty()) {
                    Text(
                        text = "Swipe your finger across all grid boxes",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val coveragePercent = (touchedCells.size * 100 / totalCells)
            TestPassFailButtons(
                onPass = {
                    onRecordResult(true, "Digitizer touch grid: $coveragePercent% responsive")
                },
                onFail = {
                    onRecordResult(false, "Digitizer dead zones encountered ($coveragePercent% covered)")
                },
                onSkip = { onRecordResult(true, "Digitizer grid skipped") }
            )
        }
    }
}
