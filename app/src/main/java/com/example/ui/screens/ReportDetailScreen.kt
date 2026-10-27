package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TestCategory
import com.example.data.model.TestStatus
import com.example.ui.AppScreen
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticCard
import com.example.ui.components.DiagnosticTopAppBar
import com.example.ui.components.HealthScoreRing
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
fun ReportDetailScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val testItems by viewModel.testItems.collectAsStateWithLifecycle()
    val specs by viewModel.deviceSpecs.collectAsStateWithLifecycle()
    val battery by viewModel.batteryState.collectAsStateWithLifecycle()
    val lastSavedId by viewModel.lastSavedReportId.collectAsStateWithLifecycle()

    val health = remember(testItems) { viewModel.calculateOverallHealth() }
    var isSavedLocally by remember { mutableStateOf(lastSavedId != null) }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Hardware Diagnostic Report",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("nav_to_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = CyanAccent
                        )
                    }
                }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Health Score Hero Banner
            Surface(
                color = DiagnosticSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OVERALL HEALTH ASSESSMENT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    HealthScoreRing(
                        score = health.score,
                        rating = health.rating,
                        sizeDp = 150
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricPill("Passed", "${health.passedCount}", GreenSuccess)
                        MetricPill("Failed", "${health.failedCount}", RedDanger)
                        MetricPill("Warnings", "${health.warningCount}", AmberWarning)
                        MetricPill("Total", "${health.totalCount}", TextSecondary)
                    }
                }
            }

            // Quick Action Buttons (Share, Save, Copy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val summary = viewModel.generateShareableSummary()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, summary)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Diagnostic Report"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_report_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (!isSavedLocally) {
                            viewModel.saveReportToHistory("Comprehensive report")
                            isSavedLocally = true
                            val toastMsg = if (viewModel.currentUser.value != null) {
                                "Saved to local history & synced to Firebase Cloud ☁️"
                            } else {
                                "Report saved to local history"
                            }
                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_report_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSavedLocally) GreenSuccess else DiagnosticSurfaceVariant,
                        contentColor = if (isSavedLocally) Color.Black else TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isSavedLocally) Icons.Default.BookmarkAdded else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSavedLocally) "Saved" else "Save", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val summary = viewModel.generateShareableSummary()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic Report", summary))
                        Toast.makeText(context, "Copied report to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Device Identification Specs
            DiagnosticCard(
                title = "Device Identity & Hardware Specs",
                subtitle = "Hardware identity and operating system profile"
            ) {
                ReportSpecRow("Device Model:", "${specs.manufacturer} ${specs.model}")
                ReportSpecRow("Hardware Platform:", "${specs.device} (${specs.board})")
                ReportSpecRow("Operating System:", "Android ${specs.androidVersion} (API ${specs.sdkInt})")
                ReportSpecRow("Display Panel:", "${specs.displayResolution} @ ${specs.displayRefreshRate}Hz")
                ReportSpecRow("Operating RAM:", "${specs.ramAvailableGb} GB free / ${specs.ramTotalGb} GB total")
                ReportSpecRow("Internal Storage:", "${specs.storageAvailableGb} GB free / ${specs.storageTotalGb} GB total")
                ReportSpecRow("Battery Condition:", "${battery.levelPercent}% • ${battery.health} • ${battery.temperatureCelsius}°C")
            }

            // Category Breakdown Items
            TestCategory.entries.forEach { category ->
                val categoryItems = testItems.filter { it.category == category }
                if (categoryItems.isNotEmpty()) {
                    DiagnosticCard(
                        title = category.displayName,
                        subtitle = category.iconDescription
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categoryItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        if (item.detailMessage.isNotEmpty()) {
                                            Text(
                                                text = item.detailMessage,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TestStatusBadge(status = item.status)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Retest All Button
            OutlinedButton(
                onClick = {
                    viewModel.resetAllTests()
                    viewModel.evaluateAutomaticSystemHealth()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset & Retest All Diagnostics", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
fun ReportSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
