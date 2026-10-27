package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.auth.GoogleSignInHelper
import android.widget.Toast
import androidx.compose.runtime.remember
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
import com.example.data.model.TestCategory
import com.example.data.model.TestStatus
import com.example.ui.AppScreen
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticTopAppBar
import com.example.ui.components.HealthScoreRing
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
fun HomeScreen(
    viewModel: DiagnosticViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val scrollState = rememberScrollState()
    val testItems by viewModel.testItems.collectAsStateWithLifecycle()
    val battery by viewModel.batteryState.collectAsStateWithLifecycle()
    val specs by viewModel.deviceSpecs.collectAsStateWithLifecycle()

    val health = remember(testItems) { viewModel.calculateOverallHealth() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val credentialManager = remember { androidx.credentials.CredentialManager.create(context) }
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showNoAccountDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.evaluateAutomaticSystemHealth()
        GoogleSignInHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = coroutineScope,
            onAccountSelected = { email, name, photo, token ->
                viewModel.setUserAccount(email, name, photo, token)
            },
            onAuthSuccess = {},
            onUnauthenticated = {}
        )
    }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Hardware Diagnostics",
                actions = {
                    if (userAccount == null) {
                        OutlinedButton(
                            onClick = {
                                GoogleSignInHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = coroutineScope,
                                    onAccountSelected = { email, name, photo, token ->
                                        viewModel.setUserAccount(email, name, photo, token)
                                    },
                                    onAuthSuccess = {
                                        Toast.makeText(context, "Signed in as ${viewModel.userAccount.value?.email}", Toast.LENGTH_SHORT).show()
                                    },
                                    onAuthError = { err: String ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    },
                                    onNoCredentials = {
                                        showNoAccountDialog = true
                                    }
                                )
                            },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("google_signin_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            color = GreenSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .clickable { showSignOutDialog = true }
                                .padding(horizontal = 4.dp)
                                .testTag("signed_in_account_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(GreenSuccess),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (userAccount?.email?.take(1) ?: "U").uppercase(),
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = userAccount?.email?.substringBefore("@") ?: "Synced",
                                    fontSize = 11.sp,
                                    color = GreenSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onNavigate(AppScreen.HISTORY) },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Diagnostic History",
                            tint = CyanAccent
                        )
                    }
                }
            )
        },
        containerColor = DiagnosticDarkBg
    ) { innerPadding ->
        if (showSignOutDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Google Account Connected", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Account: ${userAccount?.displayName ?: "User"}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Email: ${userAccount?.email}",
                            color = CyanAccent,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cloud Firestore sync is active. Your hardware diagnostic scans and reports are securely backed up to the cloud under this account.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            viewModel.clearUserAccount()
                            GoogleSignInHelper.signOut(context, credentialManager, coroutineScope) {
                                Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                    ) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSignOutDialog = false }) {
                        Text("Close")
                    }
                },
                containerColor = DiagnosticSurface
            )
        }

        if (showNoAccountDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showNoAccountDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Google Account Required / حساب Google",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "No Google Account is signed in on this device or emulator.",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "• Local Mode: All phone diagnostics (Screen, Battery, Speaker, Audio, Haptics, Sensors, Cameras) and report generation work 100% offline without signing in.",
                            color = GreenSuccess,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Cloud Sync: To backup reports to Firebase Cloud, you can add a Google account in Android Settings (Settings > Passwords & Accounts).",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoAccountDialog = false
                            GoogleSignInHelper.openAccountSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showNoAccountDialog = false }) {
                        Text("Use Offline Mode")
                    }
                },
                containerColor = DiagnosticSurface
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Health Score & Device Overview Banner
            Surface(
                color = DiagnosticSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${specs.manufacturer} ${specs.model}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Android ${specs.androidVersion} • API ${specs.sdkInt}",
                                fontSize = 12.sp,
                                color = CyanAccent
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "DIAGNOSTICS READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    HealthScoreRing(
                        score = health.score,
                        rating = health.rating,
                        sizeDp = 136
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(AppScreen.FULL_SCAN) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("run_full_scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Hardware Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.evaluateAutomaticSystemHealth()
                                onNavigate(AppScreen.REPORT_DETAIL)
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("generate_report_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Report", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Interactive Guided Phone Test Hero Card
            Surface(
                color = DiagnosticSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigate(AppScreen.INTERACTIVE_INSPECTION) }
                    .testTag("interactive_inspection_hero_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Personal Interactive Test",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GreenSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HANDS-ON",
                                    color = GreenSuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Personally test touch grid, colors, volume keys, earpiece, speakers, mic, cameras & sensors",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Real-Time Telemetry Quick Strip
            Text(
                text = "REAL-TIME HARDWARE METRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTelemetryCard(
                    title = "Battery",
                    value = "${battery.levelPercent}%",
                    status = "${battery.temperatureCelsius}°C",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = if (battery.levelPercent > 20) GreenSuccess else RedDanger,
                    modifier = Modifier.weight(1f)
                )
                QuickTelemetryCard(
                    title = "Display",
                    value = "${specs.displayRefreshRate.toInt()} Hz",
                    status = specs.displayResolution.substringBefore(" px"),
                    icon = Icons.Default.TouchApp,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTelemetryCard(
                    title = "RAM Memory",
                    value = "${specs.ramAvailableGb} GB",
                    status = "Free of ${specs.ramTotalGb}GB",
                    icon = Icons.Default.Memory,
                    accentColor = Color(0xFFE040FB),
                    modifier = Modifier.weight(1f)
                )
                QuickTelemetryCard(
                    title = "Storage",
                    value = "${specs.storageAvailableGb} GB",
                    status = "Free of ${specs.storageTotalGb}GB",
                    icon = Icons.Default.Storage,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Component Tests Section
            Text(
                text = "COMPONENT DIAGNOSTIC SUITE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            ComponentCategoryCard(
                title = "Wireless Radios & Network",
                description = "Wi-Fi interface, Bluetooth LE, cellular modem, GPS GNSS and NFC controller",
                icon = Icons.Default.Wifi,
                category = TestCategory.CONNECTIVITY,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.CONNECTIVITY_TEST) }
            )

            ComponentCategoryCard(
                title = "Camera & Optical Sensors",
                description = "Live viewfinder preview, front/rear camera switch, optical strobe flash check",
                icon = Icons.Default.CameraAlt,
                category = TestCategory.CAMERA,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.CAMERA_TEST) }
            )

            ComponentCategoryCard(
                title = "Processor, RAM & Storage",
                description = "CPU core benchmark, RAM buffer allocation and flash memory I/O speed",
                icon = Icons.Default.Speed,
                category = TestCategory.SYSTEM_INFO,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.SYSTEM_TEST) }
            )

            ComponentCategoryCard(
                title = "Screen & Touch Display",
                description = "Dead pixel inspection, multi-touch tracking, touch digitizer grid",
                icon = Icons.Default.TouchApp,
                category = TestCategory.SCREEN,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.SCREEN_TEST) }
            )

            ComponentCategoryCard(
                title = "Battery & Power Health",
                description = "Charging circuitry, cell health, operating voltage and thermal safety",
                icon = Icons.Default.ElectricBolt,
                category = TestCategory.BATTERY,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.BATTERY_TEST) }
            )

            ComponentCategoryCard(
                title = "Speaker & Microphone Audio",
                description = "Stereo channels, 200Hz-4kHz sweep, decibel level waveform",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                category = TestCategory.AUDIO,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.AUDIO_TEST) }
            )

            ComponentCategoryCard(
                title = "Motion & Ambient Sensors",
                description = "Accelerometer 3D spirit level, digital compass, lux meter, proximity",
                icon = Icons.Default.Sensors,
                category = TestCategory.SENSORS,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.SENSORS_TEST) }
            )

            ComponentCategoryCard(
                title = "Haptics & Camera Torch",
                description = "Vibration actuator patterns and rear LED illumination",
                icon = Icons.Default.Vibration,
                category = TestCategory.HAPTICS_TORCH,
                testItems = testItems,
                onClick = { onNavigate(AppScreen.HAPTICS_TORCH_TEST) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuickTelemetryCard(
    title: String,
    value: String,
    status: String,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextSecondary)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(text = status, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
fun ComponentCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    category: TestCategory,
    testItems: List<com.example.data.model.DiagnosticItem>,
    onClick: () -> Unit
) {
    val items = testItems.filter { it.category == category }
    val passedCount = items.count { it.status == TestStatus.PASSED }
    val failedCount = items.count { it.status == TestStatus.FAILED }
    val hasUntested = items.any { it.status == TestStatus.UNTESTED }

    val statusBadgeColor = when {
        failedCount > 0 -> RedDanger
        passedCount > 0 && !hasUntested -> GreenSuccess
        passedCount > 0 -> CyanAccent
        else -> TextMuted
    }

    val statusText = when {
        failedCount > 0 -> "$failedCount Failed"
        passedCount == items.size -> "All Passed ($passedCount/${items.size})"
        passedCount > 0 -> "$passedCount/${items.size} Tested"
        else -> "Untested (${items.size} checks)"
    }

    Surface(
        color = DiagnosticSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DiagnosticSurfaceVariant)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusBadgeColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Test",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
