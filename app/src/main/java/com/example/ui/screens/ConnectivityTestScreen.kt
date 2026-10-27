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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RadioInfo
import com.example.data.model.TestStatus
import com.example.ui.DiagnosticViewModel
import com.example.ui.components.DiagnosticTopAppBar
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

@Composable
fun ConnectivityTestScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val radio by viewModel.radioInfo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Wireless Radios & Network",
                onBack = onBack
            )
        },
        containerColor = DiagnosticDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RADIO CONTROLLERS & INTERFACES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Wi-Fi Card
            RadioDetailCard(
                title = "Wi-Fi Adapter",
                status = if (radio.wifiConnected) "Connected & Online" else if (radio.wifiEnabled) "Enabled (Idle)" else "Disabled",
                icon = Icons.Default.Wifi,
                accentColor = if (radio.wifiConnected) GreenSuccess else if (radio.wifiEnabled) CyanAccent else TextMuted,
                details = listOf(
                    "Network State" to if (radio.wifiConnected) "Active Connection" else "Not Connected",
                    "Adapter State" to if (radio.wifiEnabled) "Powered On" else "Off",
                    "Details" to radio.wifiDetails.ifEmpty { "Standard 802.11 dual-band radio" }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bluetooth Card
            RadioDetailCard(
                title = "Bluetooth Controller",
                status = if (radio.bluetoothEnabled) "Powered On (Scanning)" else if (radio.bluetoothAvailable) "Available (Off)" else "Not Supported",
                icon = Icons.Default.Bluetooth,
                accentColor = if (radio.bluetoothEnabled) CyanAccent else AmberWarning,
                details = listOf(
                    "Bluetooth LE (BLE)" to if (radio.bluetoothBleSupported) "Supported (Low Energy)" else "Standard Only",
                    "Status" to radio.bluetoothDetails.ifEmpty { "Ready for device pairing" }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cellular Card
            RadioDetailCard(
                title = "Cellular Baseband Modem",
                status = if (radio.cellularConnected) "Data Connected" else if (radio.cellularAvailable) "Modem Standby" else "No Telephony",
                icon = Icons.Default.SignalCellularAlt,
                accentColor = if (radio.cellularConnected) GreenSuccess else AmberWarning,
                details = listOf(
                    "Modem Hardware" to if (radio.cellularAvailable) "SIM Radio Present" else "Wi-Fi Only Device",
                    "Connection" to radio.cellularDetails.ifEmpty { "Cellular radio active" }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GPS Card
            RadioDetailCard(
                title = "GPS / GNSS Satellite Receiver",
                status = if (radio.gpsEnabled) "Location Provider Active" else if (radio.gpsAvailable) "GPS Hardware Present" else "GPS Not Available",
                icon = Icons.Default.GpsFixed,
                accentColor = if (radio.gpsEnabled) GreenSuccess else CyanAccent,
                details = listOf(
                    "GNSS Positioning" to if (radio.gpsAvailable) "Multi-Constellation Satellite Ready" else "Disabled"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // NFC Card
            RadioDetailCard(
                title = "NFC Controller",
                status = if (radio.nfcEnabled) "Active (Ready to Read)" else if (radio.nfcAvailable) "Hardware Present" else "Not Present",
                icon = Icons.Default.Nfc,
                accentColor = if (radio.nfcEnabled) GreenSuccess else if (radio.nfcAvailable) AmberWarning else TextMuted,
                details = listOf(
                    "Contactless Communication" to if (radio.nfcAvailable) "NFC Chip Present" else "No NFC Chip"
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.hardwareManager.refreshRadioInfo()
                    viewModel.evaluateAutomaticSystemHealth()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("connectivity_refresh_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm All Radios Tested", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RadioDetailCard(
    title: String,
    status: String,
    icon: ImageVector,
    accentColor: Color,
    details: List<Pair<String, String>>
) {
    Surface(
        color = DiagnosticSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = status, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            details.forEach { (k, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = k, color = TextSecondary, fontSize = 11.sp)
                    Text(text = v, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
