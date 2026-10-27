package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.DiagnosticViewModel
import com.example.ui.screens.AudioTestScreen
import com.example.ui.screens.BatteryHealthScreen
import com.example.ui.screens.FullScanScreen
import com.example.ui.screens.HapticsTorchScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.ReportHistoryScreen
import com.example.ui.screens.ScreenTestScreen
import com.example.ui.screens.SensorsTestScreen
import com.example.ui.theme.DiagnosticDarkBg
import com.example.ui.theme.MyApplicationTheme

import android.view.KeyEvent
import com.example.ui.screens.CameraTestScreen
import com.example.ui.screens.ConnectivityTestScreen
import com.example.ui.screens.InteractiveInspectionScreen
import com.example.ui.screens.SystemHardwareScreen

class MainActivity : ComponentActivity() {

    private val viewModel: DiagnosticViewModel by viewModels()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.currentScreen.value == AppScreen.INTERACTIVE_INSPECTION) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                viewModel.onPhysicalKeyPressed(keyCode)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                BackHandler(enabled = currentScreen != AppScreen.HOME) {
                    viewModel.navigateBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DiagnosticDarkBg
                ) {
                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            AppScreen.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { viewModel.navigateTo(it) }
                            )
                            AppScreen.FULL_SCAN -> FullScanScreen(
                                viewModel = viewModel,
                                onFinish = { viewModel.navigateTo(AppScreen.REPORT_DETAIL) },
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.INTERACTIVE_INSPECTION -> InteractiveInspectionScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.CAMERA_TEST -> CameraTestScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.CONNECTIVITY_TEST -> ConnectivityTestScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.SYSTEM_TEST -> SystemHardwareScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.SCREEN_TEST -> ScreenTestScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.BATTERY_TEST -> BatteryHealthScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.AUDIO_TEST -> AudioTestScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.SENSORS_TEST -> SensorsTestScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.HAPTICS_TORCH_TEST -> HapticsTorchScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.REPORT_DETAIL -> ReportDetailScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() },
                                onNavigateToHistory = { viewModel.navigateTo(AppScreen.HISTORY) }
                            )
                            AppScreen.HISTORY -> ReportHistoryScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
