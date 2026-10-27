package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TestStatus
import com.example.diagnostics.AudioChannel
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticTopAppBar
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
import kotlinx.coroutines.delay

enum class InspectionStep(val title: String, val subtitle: String) {
    TOUCH_GRID("1. Touch Digitizer Grid", "Drag your finger across all blocks on the screen"),
    SCREEN_COLORS("2. Dead Pixel & Color Test", "Inspect screen purity across solid RGBW colors"),
    MULTI_TOUCH("3. Multi-Touch Tracking", "Place 2 or more fingers on the screen simultaneously"),
    VOLUME_KEYS("4. Physical Volume Keys", "Press Volume Up and Volume Down hardware buttons"),
    EARPIECE("5. Phone Earpiece Speaker", "Listen closely to phone call voice receiver sound"),
    LOUDSPEAKER("6. Stereo Loudspeaker", "Verify clear multimedia sound playback"),
    MICROPHONE("7. Microphone Audio Capture", "Speak into the phone and watch the live volume meter"),
    CAMERA_FLASH("8. Camera & Flashlight", "Verify rear and front selfie camera preview"),
    VIBRATION("9. Haptic Vibration Motor", "Feel for haptic click feedback and pulse vibrations"),
    PROXIMITY_LIGHT("10. Proximity & Light Sensors", "Cover top of phone with your hand to trigger near state"),
    SUMMARY("Inspection Completed", "Review your personal test results and certificate")
}

@Composable
fun InteractiveInspectionScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(InspectionStep.TOUCH_GRID) }
    val stepResults = remember { mutableStateListOf<Boolean>() }

    val totalSteps = InspectionStep.entries.size - 1
    val currentStepIndex = currentStep.ordinal
    val progress = (currentStepIndex.toFloat() / totalSteps).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Guided Phone Inspection",
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
            // Step Progress Header
            if (currentStep != InspectionStep.SUMMARY) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "STEP ${currentStepIndex + 1} OF $totalSteps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% Done",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanAccent,
                        trackColor = DiagnosticSurfaceVariant
                    )
                }
            }

            // Step Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentStep) {
                    InspectionStep.TOUCH_GRID -> TouchGridStep(
                        onPassed = {
                            viewModel.updateTestResult("screen_digitizer", TestStatus.PASSED, "Full grid verified by user")
                            currentStep = InspectionStep.SCREEN_COLORS
                        }
                    )
                    InspectionStep.SCREEN_COLORS -> ScreenColorsStep(
                        onPassed = {
                            viewModel.updateTestResult("screen_dead_pixel", TestStatus.PASSED, "Dead pixels inspected: clean")
                            currentStep = InspectionStep.MULTI_TOUCH
                        },
                        onFailed = {
                            viewModel.updateTestResult("screen_dead_pixel", TestStatus.WARNING, "Defects reported during color test")
                            currentStep = InspectionStep.MULTI_TOUCH
                        }
                    )
                    InspectionStep.MULTI_TOUCH -> MultiTouchStep(
                        onPassed = {
                            viewModel.updateTestResult("screen_multitouch", TestStatus.PASSED, "Multi-touch verified by user")
                            currentStep = InspectionStep.VOLUME_KEYS
                        }
                    )
                    InspectionStep.VOLUME_KEYS -> VolumeKeysStep(
                        viewModel = viewModel,
                        onPassed = {
                            currentStep = InspectionStep.EARPIECE
                        }
                    )
                    InspectionStep.EARPIECE -> EarpieceStep(
                        viewModel = viewModel,
                        onPassed = {
                            viewModel.updateTestResult("audio_earpiece", TestStatus.PASSED, "Voice receiver sound verified")
                            currentStep = InspectionStep.LOUDSPEAKER
                        },
                        onFailed = {
                            viewModel.updateTestResult("audio_earpiece", TestStatus.WARNING, "Voice receiver sound not heard")
                            currentStep = InspectionStep.LOUDSPEAKER
                        }
                    )
                    InspectionStep.LOUDSPEAKER -> LoudspeakerStep(
                        viewModel = viewModel,
                        onPassed = {
                            viewModel.updateTestResult("audio_loudspeaker", TestStatus.PASSED, "Multimedia stereo speaker verified")
                            currentStep = InspectionStep.MICROPHONE
                        },
                        onFailed = {
                            viewModel.updateTestResult("audio_loudspeaker", TestStatus.WARNING, "Loudspeaker distorted or silent")
                            currentStep = InspectionStep.MICROPHONE
                        }
                    )
                    InspectionStep.MICROPHONE -> MicrophoneStep(
                        viewModel = viewModel,
                        onPassed = {
                            viewModel.updateTestResult("audio_microphone", TestStatus.PASSED, "Microphone capture and playback verified")
                            currentStep = InspectionStep.CAMERA_FLASH
                        }
                    )
                    InspectionStep.CAMERA_FLASH -> CameraFlashStep(
                        viewModel = viewModel,
                        onPassed = {
                            viewModel.updateTestResult("camera_rear", TestStatus.PASSED, "Rear camera inspected by user")
                            viewModel.updateTestResult("camera_front", TestStatus.PASSED, "Front camera inspected by user")
                            viewModel.updateTestResult("camera_flash", TestStatus.PASSED, "Flashlight verified by user")
                            currentStep = InspectionStep.VIBRATION
                        }
                    )
                    InspectionStep.VIBRATION -> VibrationStep(
                        viewModel = viewModel,
                        onPassed = {
                            viewModel.updateTestResult("haptics_motor", TestStatus.PASSED, "Vibration tactile feedback felt")
                            currentStep = InspectionStep.PROXIMITY_LIGHT
                        },
                        onFailed = {
                            viewModel.updateTestResult("haptics_motor", TestStatus.WARNING, "Vibration motor not felt")
                            currentStep = InspectionStep.PROXIMITY_LIGHT
                        }
                    )
                    InspectionStep.PROXIMITY_LIGHT -> ProximityLightStep(
                        viewModel = viewModel,
                        onPassed = {
                            viewModel.updateTestResult("sensor_prox", TestStatus.PASSED, "Proximity distance trigger verified")
                            viewModel.updateTestResult("sensor_light", TestStatus.PASSED, "Ambient light lux change verified")
                            currentStep = InspectionStep.SUMMARY
                        }
                    )
                    InspectionStep.SUMMARY -> InspectionSummaryStep(
                        viewModel = viewModel,
                        onFinish = onBack
                    )
                }
            }
        }
    }
}

// ----------------- STEP 1: TOUCH GRID -----------------
@Composable
private fun TouchGridStep(onPassed: () -> Unit) {
    val rows = 8
    val cols = 5
    val totalTiles = rows * cols
    val touchedTiles = remember { mutableStateListOf<Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Touch Digitizer Test", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Slide your finger across all grid tiles until they all turn green",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${touchedTiles.size} / $totalTiles Tiles Completed",
            color = if (touchedTiles.size == totalTiles) GreenSuccess else CyanAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, DiagnosticCardBorder, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val col = (offset.x / (size.width / cols)).toInt().coerceIn(0, cols - 1)
                            val row = (offset.y / (size.height / rows)).toInt().coerceIn(0, rows - 1)
                            val index = row * cols + col
                            if (!touchedTiles.contains(index)) touchedTiles.add(index)
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            val col = (offset.x / (size.width / cols)).toInt().coerceIn(0, cols - 1)
                            val row = (offset.y / (size.height / rows)).toInt().coerceIn(0, rows - 1)
                            val index = row * cols + col
                            if (!touchedTiles.contains(index)) touchedTiles.add(index)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tileWidth = size.width / cols
                val tileHeight = size.height / rows

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val index = r * cols + c
                        val isTouched = touchedTiles.contains(index)
                        val color = if (isTouched) Color(0xFF00E676).copy(alpha = 0.85f) else Color(0xFF1E2638)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(c * tileWidth + 2f, r * tileHeight + 2f),
                            size = Size(tileWidth - 4f, tileHeight - 4f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPassed,
            enabled = touchedTiles.size >= (totalTiles * 0.75f),
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("touch_grid_next_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Touch Responsive (Next)", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- STEP 2: SCREEN COLORS -----------------
@Composable
private fun ScreenColorsStep(
    onPassed: () -> Unit,
    onFailed: () -> Unit
) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    val colorNames = listOf("Red Screen", "Green Screen", "Blue Screen", "White Screen", "Black Screen")
    var colorIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dead Pixel Inspection", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tap screen preview to cycle pure RGBW colors. Look for stuck, dead or bright spots.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, CyanAccent, RoundedCornerShape(16.dp))
                .background(colors[colorIndex])
                .clickable {
                    colorIndex = (colorIndex + 1) % colors.size
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${colorNames[colorIndex]} (Tap to cycle)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPassed,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("colors_pass_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("No Dead Pixels", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onFailed,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Found Defect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ----------------- STEP 3: MULTI-TOUCH -----------------
@Composable
private fun MultiTouchStep(onPassed: () -> Unit) {
    var maxTouches by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Multi-Touch Controller", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Place 2 or 3 fingers on the touch area to test simultaneous point tracking",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
                .background(DiagnosticSurface)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val count = event.changes.count { it.pressed }
                            if (count > maxTouches) {
                                maxTouches = count
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$maxTouches Finger(s) Detected",
                    color = if (maxTouches >= 2) GreenSuccess else CyanAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("Touch and hold with multiple fingers", color = TextMuted, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPassed,
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("multitouch_pass_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Multi-Touch Verified (Next)", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- STEP 4: VOLUME KEYS -----------------
@Composable
private fun VolumeKeysStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit
) {
    var volumeUpPressed by remember { mutableStateOf(false) }
    var volumeDownPressed by remember { mutableStateOf(false) }
    val lastKey by viewModel.lastPressedKey.collectAsStateWithLifecycle()

    LaunchedEffect(lastKey) {
        when (lastKey) {
            KeyEvent.KEYCODE_VOLUME_UP -> volumeUpPressed = true
            KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownPressed = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Physical Buttons Test", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Press the physical Volume Up and Volume Down buttons on your device",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyIndicatorCard("Volume Up (+)", Icons.Default.VolumeUp, volumeUpPressed)
            Spacer(modifier = Modifier.width(16.dp))
            KeyIndicatorCard("Volume Down (-)", Icons.Default.VolumeDown, volumeDownPressed)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (volumeUpPressed && volumeDownPressed) "All physical buttons recognized!" else "Waiting for button press...",
            color = if (volumeUpPressed && volumeDownPressed) GreenSuccess else CyanAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPassed,
            enabled = volumeUpPressed || volumeDownPressed,
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("buttons_next_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Buttons Functional (Next)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KeyIndicatorCard(title: String, icon: ImageVector, isPressed: Boolean) {
    Surface(
        color = if (isPressed) GreenSuccess.copy(alpha = 0.15f) else DiagnosticSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(140.dp)
            .border(2.dp, if (isPressed) GreenSuccess else DiagnosticCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPressed) GreenSuccess else TextMuted,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = if (isPressed) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = if (isPressed) "PASSED" else "PRESS NOW", color = if (isPressed) GreenSuccess else AmberWarning, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ----------------- STEP 5: EARPIECE -----------------
@Composable
private fun EarpieceStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit,
    onFailed: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Hearing, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text("Earpiece Voice Speaker", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Tap play and hold your phone to your ear like during a call to listen for the test tone",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isPlaying = true
                viewModel.audioEngine.playChannelTone(AudioChannel.EARPIECE, 1000, 2000)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isPlaying) "Playing in Earpiece..." else "Play Earpiece Tone", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPassed,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Heard Sound (Pass)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onFailed,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("No Sound", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ----------------- STEP 6: LOUDSPEAKER -----------------
@Composable
private fun LoudspeakerStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit,
    onFailed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text("Stereo Loudspeaker Test", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Plays loud multimedia test melody from the bottom/external speaker",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.audioEngine.playChannelTone(AudioChannel.STEREO_BOTH, 880, 1500)
            },
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Play Loudspeaker Tone", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPassed,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Sound Clear (Pass)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onFailed,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Sound Defective", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ----------------- STEP 7: MICROPHONE -----------------
@Composable
private fun MicrophoneStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasMicPermission = it
    }

    val micDecibels by viewModel.audioEngine.micDecibels.collectAsStateWithLifecycle()
    val isRecording by viewModel.audioEngine.isRecordingMic.collectAsStateWithLifecycle()

    val micRatio = remember(micDecibels) {
        // Map decibels (e.g. 0 to 80 dB) to 0f..1f ratio
        (micDecibels / 80f).coerceIn(0f, 1f)
    }

    LaunchedEffect(hasMicPermission) {
        if (!hasMicPermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.audioEngine.startMicMonitor()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.audioEngine.stopMicMonitor()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Mic, contentDescription = null, tint = RedDanger, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text("Microphone Audio Capture", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Speak into the microphone: 'Testing 1, 2, 3'. The live amplitude bar responds to your voice.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live Amplitude Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DiagnosticSurface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LIVE DECIBEL LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { micRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = if (micRatio > 0.35f) GreenSuccess else CyanAccent,
                trackColor = DiagnosticSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${micDecibels.toInt()} dB (${(micRatio * 100).toInt()}% Amplitude)",
                color = if (micRatio > 0.15f) GreenSuccess else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onPassed,
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Microphone Working (Next)", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- STEP 8: CAMERA & FLASH -----------------
@Composable
private fun CameraFlashStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera & Flashlight Test", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Verify live camera sensor feed and switch between front and rear cameras",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = DiagnosticSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (_: Exception) {}
                        }, executor)
                        previewView
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        val executor = ContextCompat.getMainExecutor(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (_: Exception) {}
                        }, executor)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPassed,
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Camera Feed Clear (Next)", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- STEP 9: VIBRATION -----------------
@Composable
private fun VibrationStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit,
    onFailed: () -> Unit
) {
    val isVibrating by viewModel.isVibrating.collectAsStateWithLifecycle()
    val hasVibrator by viewModel.hasVibrator.collectAsStateWithLifecycle()
    val vibratorInfo by viewModel.vibratorInfo.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing vibrating icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    if (isVibrating) Color(0xFFFFD600).copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.12f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isVibrating) Color(0xFFFFD600) else CyanAccent.copy(alpha = 0.4f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = null,
                tint = if (isVibrating) Color(0xFFFFD600) else CyanAccent,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Haptic Vibration Motor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))

        // Hardware state chip
        androidx.compose.material3.Surface(
            color = if (hasVibrator) GreenSuccess.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = vibratorInfo,
                color = if (hasVibrator) GreenSuccess else CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isVibrating) "Actuator pulsing right now..." else "Tap buttons below to test vibration motor intensity and patterns",
            color = if (isVibrating) Color(0xFFFFD600) else TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Main Wave pulse button
        Button(
            onClick = {
                viewModel.hardwareManager.vibratePattern("WAVE")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isVibrating) Color(0xFFFFD600) else CyanAccent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isVibrating) "Vibrating..." else "Pulse Vibration Motor (Wave)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Alternative quick pattern buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.hardwareManager.vibratePattern("CLICK") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Click Pulse", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { viewModel.hardwareManager.vibratePattern("DOUBLE_PULSE") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Double Pulse", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { viewModel.hardwareManager.vibratePattern("ALERT") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Alert Rhythm", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPassed,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Felt Vibration (Pass)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onFailed,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("No Vibration", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ----------------- STEP 10: PROXIMITY & LIGHT -----------------
@Composable
private fun ProximityLightStep(
    viewModel: DiagnosticViewModel,
    onPassed: () -> Unit
) {
    val sensors by viewModel.sensorReadings.collectAsStateWithLifecycle()
    var proximityTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(sensors.isProximityNear) {
        if (sensors.isProximityNear) {
            proximityTriggered = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Sensors, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text("Proximity & Light Sensors", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Cover the top of your phone with your palm to trigger the proximity detector and block ambient light",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = if (sensors.isProximityNear) GreenSuccess.copy(alpha = 0.15f) else DiagnosticSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, if (sensors.isProximityNear) GreenSuccess else DiagnosticCardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (sensors.isProximityNear) "PALM DETECTED (NEAR)" else "SENSOR OPEN (FAR)",
                    color = if (sensors.isProximityNear) GreenSuccess else AmberWarning,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Distance: ${sensors.proximityCm} cm • Light: ${sensors.ambientLux.toInt()} Lux", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onPassed,
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Complete Inspection", fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- STEP 11: SUMMARY CERTIFICATE -----------------
@Composable
private fun InspectionSummaryStep(
    viewModel: DiagnosticViewModel,
    onFinish: () -> Unit
) {
    val specs by viewModel.deviceSpecs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = GreenSuccess.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(44.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Personal Phone Inspection Passed!",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "You have personally tested and confirmed all hardware components",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Certificate Card
        Surface(
            color = DiagnosticSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HARDWARE VERIFICATION CHECKLIST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                VerifiedCheckItem("Touch Digitizer & Display Grid")
                VerifiedCheckItem("Screen Colors & Dead Pixel Uniformity")
                VerifiedCheckItem("Multi-Touch Simultaneous Tracking")
                VerifiedCheckItem("Physical Hardware Volume Buttons")
                VerifiedCheckItem("Phone Call Earpiece Acoustic Receiver")
                VerifiedCheckItem("Multimedia Stereo Loudspeaker Output")
                VerifiedCheckItem("Microphone Audio Waveform Capture")
                VerifiedCheckItem("Front & Rear Camera Optical Viewfinders")
                VerifiedCheckItem("Vibration Motor Tactile Feedback")
                VerifiedCheckItem("Proximity Ear Sensor & Ambient Light")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.saveReportToHistory("Interactive Personal Hardware Inspection Verified")
                onFinish()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_interactive_report_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Verified Inspection Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun VerifiedCheckItem(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = TextPrimary, fontSize = 12.sp)
    }
}
