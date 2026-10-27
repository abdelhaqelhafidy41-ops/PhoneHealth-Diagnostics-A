package com.example.data.model

enum class TestCategory(val displayName: String, val iconDescription: String) {
    CONNECTIVITY("Connectivity & Radios", "Wi-Fi, Bluetooth, Cellular, GPS & NFC"),
    SCREEN("Screen & Touch", "Display panel and digitizer diagnostics"),
    BATTERY("Battery & Power", "Cell condition, voltage and thermal state"),
    AUDIO("Speakers & Microphones", "Loudspeaker, earpiece receiver and microphone"),
    SENSORS("Hardware Sensors", "Motion, orientation, light and proximity"),
    CAMERA("Camera & Optical", "Rear, front selfie camera and optical sensors"),
    HAPTICS_TORCH("Haptics & Torch", "Vibration motors and camera flash"),
    SYSTEM_INFO("System & Processor", "CPU cores, RAM and internal storage")
}

enum class TestStatus(val label: String) {
    UNTESTED("Not Tested"),
    RUNNING("Scanning..."),
    PASSED("Passed"),
    WARNING("Warning"),
    FAILED("Failed"),
    SKIPPED("Skipped")
}

data class DiagnosticItem(
    val id: String,
    val title: String,
    val description: String,
    val category: TestCategory,
    val status: TestStatus = TestStatus.UNTESTED,
    val detailMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class RadioInfo(
    val wifiAvailable: Boolean = false,
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val wifiDetails: String = "",
    val bluetoothAvailable: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val bluetoothBleSupported: Boolean = false,
    val bluetoothDetails: String = "",
    val cellularAvailable: Boolean = false,
    val cellularConnected: Boolean = false,
    val cellularDetails: String = "",
    val gpsAvailable: Boolean = false,
    val gpsEnabled: Boolean = false,
    val nfcAvailable: Boolean = false,
    val nfcEnabled: Boolean = false
)

data class CameraSpecs(
    val hasBackCamera: Boolean = false,
    val backCameraResolution: String = "",
    val backCameraMegapixels: Double = 0.0,
    val hasFrontCamera: Boolean = false,
    val frontCameraResolution: String = "",
    val frontCameraMegapixels: Double = 0.0,
    val hasFlash: Boolean = false,
    val totalCamerasCount: Int = 0,
    val sensorDetails: String = ""
)

data class UserAccountInfo(
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val idToken: String = "",
    val isCloudSynced: Boolean = true
)

data class BatterySnapshot(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val plugType: String = "Battery",
    val health: String = "Good",
    val temperatureCelsius: Float = 25f,
    val voltageMv: Int = 4000,
    val technology: String = "Li-ion",
    val powerStatus: String = "Discharging"
)

data class DeviceHardwareSpecs(
    val manufacturer: String = "",
    val model: String = "",
    val device: String = "",
    val board: String = "",
    val cpuAbi: String = "",
    val cpuCores: Int = 1,
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val displayResolution: String = "",
    val displayRefreshRate: Float = 60f,
    val displayDensityDpi: Int = 0,
    val ramTotalGb: Double = 0.0,
    val ramAvailableGb: Double = 0.0,
    val storageTotalGb: Double = 0.0,
    val storageAvailableGb: Double = 0.0
)

data class OverallHealth(
    val score: Int = 100, // 0 - 100
    val rating: String = "Optimal",
    val passedCount: Int = 0,
    val failedCount: Int = 0,
    val warningCount: Int = 0,
    val totalCount: Int = 0
)
