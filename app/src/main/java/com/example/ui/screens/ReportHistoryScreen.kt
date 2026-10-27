package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DiagnosticReportEntity
import com.example.data.model.FirestoreReport
import com.example.ui.DiagnosticViewModel
import com.example.ui.auth.GoogleSignInHelper
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportHistoryScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val localReports by viewModel.savedReports.collectAsStateWithLifecycle()
    val cloudReports by viewModel.cloudReports.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNoAccountDialog by remember { mutableStateOf(false) }

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
                        text = "No Google Account was found on this device or emulator.",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Local Tab: All your local hardware inspection reports are stored safely on this phone and can be reviewed in the 'On-Device History' tab without signing in.",
                        color = GreenSuccess,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Cloud Tab: To sync reports across multiple devices via Firebase, you can add a Google account in Android Settings.",
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
                    Text("Stay on Local Tab")
                }
            },
            containerColor = DiagnosticSurface
        )
    }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Diagnostic History",
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DiagnosticDarkBg,
                contentColor = CyanAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanAccent
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Local (${localReports.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cloud Sync (${if (currentUser != null) cloudReports.size else 0})", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (selectedTab == 0) {
                // Local Reports Tab
                if (localReports.isEmpty()) {
                    EmptyReportsPlaceholder(message = "No local reports saved yet")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(localReports, key = { it.id }) { report ->
                            ReportHistoryItemCard(
                                title = "Report #${report.id}",
                                rating = report.rating,
                                healthScore = report.healthScore,
                                dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(report.timestamp)),
                                passed = report.passedCount,
                                failed = report.failedCount,
                                warning = report.warningCount,
                                batteryPercent = report.batteryLevel,
                                batteryTemp = "${report.batteryTempC}°C",
                                isCloud = false,
                                onDelete = { viewModel.deleteReport(report.id) },
                                onShare = {
                                    val text = """
                                        📱 DIAGNOSTIC REPORT #${report.id}
                                        Device: ${report.deviceModel}
                                        OS: ${report.androidVersion}
                                        Health Score: ${report.healthScore}/100 [${report.rating}]
                                        Passed: ${report.passedCount} | Failed: ${report.failedCount} | Warnings: ${report.warningCount}
                                        Battery: ${report.batteryLevel}% • ${report.batteryHealth} • ${report.batteryTempC}°C
                                        Date: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(report.timestamp))}
                                    """.trimIndent()
                                    shareText(context, text, "Share Report #${report.id}")
                                }
                            )
                        }
                    }
                }
            } else {
                // Cloud Reports Tab
                if (currentUser == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Cloud Backup & Sync",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sign in with Google to backup diagnostic health reports securely to Firebase Cloud Firestore.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    GoogleSignInHelper.onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        onAccountSelected = { email, name, photo, token ->
                                            viewModel.setUserAccount(email, name, photo, token)
                                        },
                                        onAuthSuccess = {
                                            android.widget.Toast.makeText(context, "Signed in as ${viewModel.userAccount.value?.email}", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        onAuthError = { err ->
                                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                        },
                                        onNoCredentials = {
                                            showNoAccountDialog = true
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                                modifier = Modifier.height(46.dp).testTag("cloud_tab_signin_button")
                            ) {
                                Text("Sign in with Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (cloudReports.isEmpty()) {
                    EmptyReportsPlaceholder(message = "No cloud reports found for ${currentUser?.email}")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cloudReports, key = { it.id }) { report ->
                            val timestampMillis = report.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                            val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(timestampMillis))
                            ReportHistoryItemCard(
                                title = "Cloud Report",
                                rating = report.rating,
                                healthScore = report.healthScore,
                                dateStr = dateStr,
                                passed = report.passedCount,
                                failed = report.failedCount,
                                warning = report.warningCount,
                                batteryPercent = report.batteryLevel,
                                batteryTemp = "${report.batteryTempC.toFloat()}°C",
                                isCloud = true,
                                onDelete = { viewModel.deleteCloudReport(report.id) },
                                onShare = {
                                    val text = """
                                        📱 CLOUD DIAGNOSTIC REPORT
                                        ID: ${report.id}
                                        Device: ${report.deviceModel}
                                        OS: ${report.androidVersion}
                                        Health Score: ${report.healthScore}/100 [${report.rating}]
                                        Passed: ${report.passedCount} | Failed: ${report.failedCount} | Warnings: ${report.warningCount}
                                        Battery: ${report.batteryLevel}% • ${report.batteryHealth} • ${report.batteryTempC}°C
                                        Date: $dateStr
                                    """.trimIndent()
                                    shareText(context, text, "Share Cloud Report")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReportsPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Reports Available",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ReportHistoryItemCard(
    title: String,
    rating: String,
    healthScore: Int,
    dateStr: String,
    passed: Int,
    failed: Int,
    warning: Int,
    batteryPercent: Int,
    batteryTemp: String,
    isCloud: Boolean,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val scoreColor = when {
        healthScore >= 90 -> GreenSuccess
        healthScore >= 70 -> CyanAccent
        healthScore >= 50 -> AmberWarning
        else -> RedDanger
    }

    Surface(
        color = DiagnosticSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.15f))
                            .border(1.5.dp, scoreColor, CircleShape)
                    ) {
                        Text(
                            text = "$healthScore",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rating,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (isCloud) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Cloud",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = RedDanger.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DiagnosticSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pass: $passed  |  Fail: $failed  |  Warn: $warning",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Bat: $batteryPercent% ($batteryTemp)",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun shareText(context: android.content.Context, text: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, title))
}
