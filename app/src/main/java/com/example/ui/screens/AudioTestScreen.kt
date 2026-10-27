package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TestStatus
import com.example.diagnostics.AudioChannel
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
import kotlin.math.sin

@Composable
fun AudioTestScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val audioEngine = viewModel.audioEngine
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isPlayingTone by audioEngine.isPlayingTone.collectAsStateWithLifecycle()
    val currentChannel by audioEngine.currentChannel.collectAsStateWithLifecycle()
    val isSweeping by audioEngine.isSweeping.collectAsStateWithLifecycle()
    val sweepFreq by audioEngine.currentSweepFrequency.collectAsStateWithLifecycle()
    val isRecordingMic by audioEngine.isRecordingMic.collectAsStateWithLifecycle()
    val micDecibels by audioEngine.micDecibels.collectAsStateWithLifecycle()
    val micPeakDecibels by audioEngine.micPeakDecibels.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            audioEngine.startMicMonitor()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioEngine.releaseAll()
        }
    }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Speaker & Audio Diagnostics",
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
            // Stereo Speaker Channels Card
            DiagnosticCard(
                title = "Stereo Channels Check",
                subtitle = "Synthesized pure sine tone to test ear and loudspeaker separation"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeakerToneButton(
                        label = "Left (440Hz)",
                        isActive = isPlayingTone && currentChannel == AudioChannel.LEFT,
                        onClick = { audioEngine.playChannelTone(AudioChannel.LEFT, 440, 4000) },
                        modifier = Modifier.weight(1f)
                    )
                    SpeakerToneButton(
                        label = "Right (880Hz)",
                        isActive = isPlayingTone && currentChannel == AudioChannel.RIGHT,
                        onClick = { audioEngine.playChannelTone(AudioChannel.RIGHT, 880, 4000) },
                        modifier = Modifier.weight(1f)
                    )
                    SpeakerToneButton(
                        label = "Stereo Both",
                        isActive = isPlayingTone && currentChannel == AudioChannel.STEREO_BOTH,
                        onClick = { audioEngine.playChannelTone(AudioChannel.STEREO_BOTH, 660, 4000) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SpeakerToneButton(
                    label = "Earpiece Receiver Speaker (700Hz Voice Band)",
                    isActive = isPlayingTone && currentChannel == AudioChannel.EARPIECE,
                    onClick = { audioEngine.playChannelTone(AudioChannel.EARPIECE, 700, 4000) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isPlayingTone || isSweeping) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { audioEngine.stopTone() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Audio Tone", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Did loudspeakers and earpiece produce clear undistorted tone?",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult("audio_loudspeaker", TestStatus.PASSED, "Loudspeaker stereo output clear")
                        viewModel.updateTestResult("audio_earpiece", TestStatus.PASSED, "Earpiece receiver functional")
                        viewModel.updateTestResult("audio_stereo_l", TestStatus.PASSED, "Left channel clear (440Hz)")
                        viewModel.updateTestResult("audio_stereo_r", TestStatus.PASSED, "Right channel clear (880Hz)")
                    },
                    onFail = {
                        viewModel.updateTestResult("audio_loudspeaker", TestStatus.FAILED, "Speaker distortion or silence")
                        viewModel.updateTestResult("audio_earpiece", TestStatus.FAILED, "Earpiece distortion or silence")
                        viewModel.updateTestResult("audio_stereo_l", TestStatus.FAILED, "Audio channel distortion/mute")
                        viewModel.updateTestResult("audio_stereo_r", TestStatus.FAILED, "Audio channel distortion/mute")
                    },
                    onSkip = {
                        viewModel.updateTestResult("audio_loudspeaker", TestStatus.SKIPPED, "Skipped")
                        viewModel.updateTestResult("audio_earpiece", TestStatus.SKIPPED, "Skipped")
                        viewModel.updateTestResult("audio_stereo_l", TestStatus.SKIPPED, "Skipped")
                        viewModel.updateTestResult("audio_stereo_r", TestStatus.SKIPPED, "Skipped")
                    }
                )
            }

            // Frequency Sweep Card
            DiagnosticCard(
                title = "Frequency Sweep Test",
                subtitle = "Tests loudspeaker response from deep bass (200Hz) to treble (4kHz)"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSweeping) "Sweeping: $sweepFreq Hz" else "Range: 200 Hz - 4000 Hz",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSweeping) CyanAccent else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Listens for chassis rattling, distortion or dropouts",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            if (isSweeping) audioEngine.stopTone()
                            else audioEngine.playFrequencySweep(200, 4000, 4500)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSweeping) RedDanger else CyanAccent,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("sweep_button")
                    ) {
                        Icon(
                            imageVector = if (isSweeping) Icons.Default.Stop else Icons.Default.Waves,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isSweeping) "Stop" else "Sweep", fontWeight = FontWeight.Bold)
                    }
                }

                if (isSweeping) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { ((sweepFreq - 200).coerceAtLeast(0) / 3800f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyanAccent,
                        trackColor = DiagnosticSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                TestPassFailButtons(
                    onPass = {
                        viewModel.updateTestResult("audio_freq_sweep", TestStatus.PASSED, "Clean acoustic sweep 200Hz - 4kHz")
                    },
                    onFail = {
                        viewModel.updateTestResult("audio_freq_sweep", TestStatus.FAILED, "Acoustic distortion or rattling detected")
                    },
                    onSkip = {
                        viewModel.updateTestResult("audio_freq_sweep", TestStatus.SKIPPED, "Skipped")
                    }
                )
            }

            // Microphone Test Card
            DiagnosticCard(
                title = "Microphone Decibel Monitor",
                subtitle = "Real-time sound pressure level & input sensitivity"
            ) {
                if (!hasMicPermission) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Microphone permission required for audio test",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                        ) {
                            Text("Grant Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Level: ${micDecibels.toInt()} dB",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (micDecibels > 50) GreenSuccess else TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Peak Detected: ${micPeakDecibels.toInt()} dB",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = {
                                if (isRecordingMic) audioEngine.stopMicMonitor()
                                else audioEngine.startMicMonitor()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingMic) RedDanger else GreenSuccess,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("mic_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecordingMic) "Stop" else "Listen", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio visualizer wave
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070B16))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bars = 32
                            val barWidth = size.width / (bars * 1.5f)
                            val spacing = barWidth * 0.5f

                            for (i in 0 until bars) {
                                val x = i * (barWidth + spacing)
                                val normalizedDb = (micDecibels / 100f).coerceIn(0.05f, 1f)
                                val waveFactor = sin((i + System.currentTimeMillis() / 150.0) * 0.4).toFloat().coerceIn(0.2f, 1f)
                                val barHeight = size.height * normalizedDb * (if (isRecordingMic) waveFactor else 0.05f)

                                drawRoundRect(
                                    color = if (micDecibels > 60) GreenSuccess else CyanAccent,
                                    topLeft = Offset(x, (size.height - barHeight) / 2),
                                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Speak, tap near mic, or whistle to confirm input:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TestPassFailButtons(
                        onPass = {
                            val peak = micPeakDecibels.toInt()
                            viewModel.updateTestResult(
                                "audio_microphone",
                                TestStatus.PASSED,
                                "Microphone sensitive (Peak: $peak dB recorded)"
                            )
                        },
                        onFail = {
                            viewModel.updateTestResult(
                                "audio_microphone",
                                TestStatus.FAILED,
                                "No audio response detected (0 dB)"
                            )
                        },
                        onSkip = {
                            viewModel.updateTestResult("audio_microphone", TestStatus.SKIPPED, "Skipped")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SpeakerToneButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isActive) CyanAccent else DiagnosticSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(54.dp)
            .border(
                1.dp,
                if (isActive) CyanAccent else DiagnosticCardBorder,
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = if (isActive) Color.Black else TextPrimary
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
