package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TestStatus
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticCard
import com.example.ui.components.DiagnosticTopAppBar
import com.example.ui.components.TestPassFailButtons
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiagnosticDarkBg
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HapticsTorchScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hardwareManager = viewModel.hardwareManager
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val hasTorch by viewModel.hasTorch.collectAsStateWithLifecycle()
    val isVibrating by viewModel.isVibrating.collectAsStateWithLifecycle()
    val hasVibrator by viewModel.hasVibrator.collectAsStateWithLifecycle()
    val vibratorInfo by viewModel.vibratorInfo.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            hardwareManager.toggleTorch(!isTorchOn)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isTorchOn) {
                hardwareManager.toggleTorch(false)
            }
        }
    }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Haptics & Flashlight",
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
            // Vibration Card
            DiagnosticCard(
                title = "Haptic Vibration Motor",
                subtitle = "Tests internal vibration actuator strength & rhythm"
            ) {
                // Hardware status banner
                androidx.compose.material3.Surface(
                    color = if (hasVibrator) GreenSuccess.copy(alpha = 0.12f) else CyanAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (hasVibrator) GreenSuccess else CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = vibratorInfo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasVibrator) GreenSuccess else CyanAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pulsing indicator if vibrating
                if (isVibrating) {
                    androidx.compose.material3.Surface(
                        color = Color(0xFFFFD600).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vibrating Now • Actuator Pulsing...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VibrationPatternButton("Click", onClick = { hardwareManager.vibratePattern("CLICK") }, modifier = Modifier.weight(1f))
                    VibrationPatternButton("Heavy", onClick = { hardwareManager.vibratePattern("HEAVY") }, modifier = Modifier.weight(1f))
                    VibrationPatternButton("Double", onClick = { hardwareManager.vibratePattern("DOUBLE_PULSE") }, modifier = Modifier.weight(1f))
                    VibrationPatternButton("Wave", onClick = { hardwareManager.vibratePattern("WAVE") }, modifier = Modifier.weight(1f))
                    VibrationPatternButton("Alert", onClick = { hardwareManager.vibratePattern("ALERT") }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (hasVibrator) {
                        "Did your phone vibrate distinctly with each pattern?"
                    } else {
                        "Virtual emulator environment detected. Physical vibration actuates when running on real Android phones."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult("haptics_motor", TestStatus.PASSED, "Haptic motor active and responsive")
                    },
                    onFail = {
                        viewModel.updateTestResult("haptics_motor", TestStatus.FAILED, "No vibration felt")
                    },
                    onSkip = { viewModel.updateTestResult("haptics_motor", TestStatus.SKIPPED, "Skipped") }
                )
            }

            // Flashlight / Torch Card
            DiagnosticCard(
                title = "Camera Flash / Torch LED",
                subtitle = if (hasTorch) "Rear camera LED flash unit" else "Flash hardware detected/virtual"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTorchOn) "Torch LED: ON" else "Torch LED: OFF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTorchOn) Color(0xFFFFD600) else TextPrimary
                        )
                        Text(
                            text = "Toggle to check rear illumination",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isTorchOn,
                        onCheckedChange = { enable ->
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                hardwareManager.toggleTorch(enable)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFD600),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF1E2B47)
                        ),
                        modifier = Modifier.testTag("torch_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult("torch_light", TestStatus.PASSED, "Torch LED functional")
                        if (isTorchOn) hardwareManager.toggleTorch(false)
                    },
                    onFail = {
                        viewModel.updateTestResult("torch_light", TestStatus.FAILED, "Flash LED did not light up")
                        if (isTorchOn) hardwareManager.toggleTorch(false)
                    },
                    onSkip = {
                        viewModel.updateTestResult("torch_light", TestStatus.SKIPPED, "Skipped")
                        if (isTorchOn) hardwareManager.toggleTorch(false)
                    }
                )
            }
        }
    }
}

@Composable
fun VibrationPatternButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
