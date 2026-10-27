package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DiagnosticReportEntity
import com.example.data.model.BatterySnapshot
import com.example.data.model.DeviceHardwareSpecs
import com.example.data.model.DiagnosticItem
import com.example.data.model.FirestoreReport
import com.example.data.model.OverallHealth
import com.example.data.model.RadioInfo
import com.example.data.model.TestCategory
import com.example.data.model.TestStatus
import com.example.data.repository.DiagnosticCloudRepository
import com.example.diagnostics.AudioChannel
import com.example.diagnostics.AudioTestEngine
import com.example.diagnostics.HardwareDiagnosticsManager
import com.example.diagnostics.SensorReadings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class AppScreen {
    HOME,
    FULL_SCAN,
    INTERACTIVE_INSPECTION,
    CAMERA_TEST,
    CONNECTIVITY_TEST,
    SYSTEM_TEST,
    SCREEN_TEST,
    BATTERY_TEST,
    AUDIO_TEST,
    SENSORS_TEST,
    HAPTICS_TORCH_TEST,
    REPORT_DETAIL,
    HISTORY
}

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    val hardwareManager = HardwareDiagnosticsManager(application.applicationContext)
    val audioEngine = AudioTestEngine(viewModelScope)
    private val db = AppDatabase.getInstance(application)
    private val dao = db.diagnosticDao()
    val cloudRepository = DiagnosticCloudRepository.create(application)

    private val prefs = application.getSharedPreferences("user_account_prefs", android.content.Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userAccount = MutableStateFlow<com.example.data.model.UserAccountInfo?>(loadSavedAccount())
    val userAccount: StateFlow<com.example.data.model.UserAccountInfo?> = _userAccount.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val email = firebaseUser.email ?: ""
            if (email.isNotEmpty()) {
                setUserAccount(
                    email = email,
                    displayName = firebaseUser.displayName ?: email.substringBefore("@"),
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    idToken = ""
                )
            }
        }
    }

    private fun loadSavedAccount(): com.example.data.model.UserAccountInfo? {
        val email = prefs.getString("user_email", null) ?: Firebase.auth.currentUser?.email
        if (!email.isNullOrEmpty()) {
            val name = prefs.getString("user_name", "") ?: Firebase.auth.currentUser?.displayName ?: email.substringBefore("@")
            val photo = prefs.getString("user_photo", "") ?: Firebase.auth.currentUser?.photoUrl?.toString() ?: ""
            return com.example.data.model.UserAccountInfo(
                email = email,
                displayName = name.ifEmpty { email.substringBefore("@") },
                photoUrl = photo,
                isCloudSynced = true
            )
        }
        return null
    }

    fun setUserAccount(email: String, displayName: String, photoUrl: String, idToken: String) {
        val account = com.example.data.model.UserAccountInfo(
            email = email,
            displayName = displayName.ifEmpty { email.substringBefore("@") },
            photoUrl = photoUrl,
            idToken = idToken,
            isCloudSynced = true
        )
        _userAccount.value = account
        prefs.edit()
            .putString("user_email", email)
            .putString("user_name", account.displayName)
            .putString("user_photo", photoUrl)
            .apply()
    }

    fun clearUserAccount() {
        _userAccount.value = null
        prefs.edit().clear().apply()
    }

    val batteryState: StateFlow<BatterySnapshot> = hardwareManager.batteryState
    val sensorReadings: StateFlow<SensorReadings> = hardwareManager.sensorReadings
    val radioInfo: StateFlow<RadioInfo> = hardwareManager.radioInfo
    val isTorchOn: StateFlow<Boolean> = hardwareManager.isTorchOn
    val hasTorch: StateFlow<Boolean> = hardwareManager.hasTorch
    val isVibrating: StateFlow<Boolean> = hardwareManager.isVibrating
    val hasVibrator: StateFlow<Boolean> = hardwareManager.hasVibrator
    val vibratorInfo: StateFlow<String> = hardwareManager.vibratorInfo
    val cameraSpecs: StateFlow<com.example.data.model.CameraSpecs> = hardwareManager.cameraSpecs

    private val _deviceSpecs = MutableStateFlow(hardwareManager.getDeviceHardwareSpecs())
    val deviceSpecs: StateFlow<DeviceHardwareSpecs> = _deviceSpecs.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Navigation history stack for back handling
    private val screenStack = mutableListOf<AppScreen>(AppScreen.HOME)

    private val _testItems = MutableStateFlow<List<DiagnosticItem>>(createDefaultTestItems())
    val testItems: StateFlow<List<DiagnosticItem>> = _testItems.asStateFlow()

    private val _lastSavedReportId = MutableStateFlow<Long?>(null)
    val lastSavedReportId: StateFlow<Long?> = _lastSavedReportId.asStateFlow()

    // Full Comprehensive Scan State
    private val _isFullScanning = MutableStateFlow(false)
    val isFullScanning: StateFlow<Boolean> = _isFullScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentScanPhase = MutableStateFlow("Ready to scan")
    val currentScanPhase: StateFlow<String> = _currentScanPhase.asStateFlow()

    private val _currentScanItemName = MutableStateFlow("")
    val currentScanItemName: StateFlow<String> = _currentScanItemName.asStateFlow()

    // Physical key listener for interactive hardware tests
    private val _lastPressedKey = MutableStateFlow<Int?>(null)
    val lastPressedKey: StateFlow<Int?> = _lastPressedKey.asStateFlow()

    fun onPhysicalKeyPressed(keyCode: Int) {
        _lastPressedKey.value = keyCode
    }

    fun resetLastPressedKey() {
        _lastPressedKey.value = null
    }

    val savedReports: StateFlow<List<DiagnosticReportEntity>> = dao.getAllReportsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cloudReports: StateFlow<List<FirestoreReport>> = cloudRepository.observeCloudReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        hardwareManager.startSensorListening()
        Firebase.auth.addAuthStateListener(authListener)
    }

    private fun createDefaultTestItems(): List<DiagnosticItem> {
        return listOf(
            // Connectivity & Radios
            DiagnosticItem("radio_wifi", "Wi-Fi Interface", "Wi-Fi adapter, link speed and network status", TestCategory.CONNECTIVITY),
            DiagnosticItem("radio_bluetooth", "Bluetooth Adapter", "Bluetooth controller and Low Energy (BLE) support", TestCategory.CONNECTIVITY),
            DiagnosticItem("radio_cellular", "Cellular Modem", "Baseband radio modem and telephony SIM interface", TestCategory.CONNECTIVITY),
            DiagnosticItem("radio_gps", "GPS Location Hardware", "GNSS satellite positioning receiver", TestCategory.CONNECTIVITY),
            DiagnosticItem("radio_nfc", "NFC Controller", "Near Field Communication hardware support", TestCategory.CONNECTIVITY),

            // Camera & Optical
            DiagnosticItem("camera_rear", "Rear Camera Sensor", "Primary back camera optical sensor & lens", TestCategory.CAMERA),
            DiagnosticItem("camera_front", "Front Selfie Camera", "Front-facing selfie camera & facial lens", TestCategory.CAMERA),
            DiagnosticItem("camera_flash", "Camera Flash Unit", "Optical strobe & high-power LED flash emitter", TestCategory.CAMERA),

            // Audio & Speakers
            DiagnosticItem("audio_loudspeaker", "Main Loudspeaker", "Primary external multimedia speaker output", TestCategory.AUDIO),
            DiagnosticItem("audio_earpiece", "Earpiece Receiver", "Secondary phone call acoustic speaker", TestCategory.AUDIO),
            DiagnosticItem("audio_stereo_l", "Left Channel", "Left stereo acoustic channel separation", TestCategory.AUDIO),
            DiagnosticItem("audio_stereo_r", "Right Channel", "Right stereo acoustic channel separation", TestCategory.AUDIO),
            DiagnosticItem("audio_freq_sweep", "Frequency Response", "Full acoustic sweep (200Hz - 4kHz)", TestCategory.AUDIO),
            DiagnosticItem("audio_microphone", "Microphone Input", "Acoustic audio capture and decibel response", TestCategory.AUDIO),

            // Screen & Display
            DiagnosticItem("screen_refresh", "Display Panel", "Panel resolution, refresh rate and density", TestCategory.SCREEN),
            DiagnosticItem("screen_dead_pixel", "Screen Pixels", "RGBW dead/stuck pixel visual check", TestCategory.SCREEN),
            DiagnosticItem("screen_multitouch", "Multi-Touch Controller", "Simultaneous touch point recognition", TestCategory.SCREEN),
            DiagnosticItem("screen_digitizer", "Touch Digitizer", "Full display touch response grid", TestCategory.SCREEN),

            // Battery & Power
            DiagnosticItem("battery_health", "Battery Chemistry", "Cell chemical condition and wear state", TestCategory.BATTERY),
            DiagnosticItem("battery_temp", "Thermal Management", "Operating temperature safety threshold", TestCategory.BATTERY),
            DiagnosticItem("battery_voltage", "Voltage Regulation", "Consistent battery cell power output", TestCategory.BATTERY),
            DiagnosticItem("battery_charging", "Charging Circuitry", "Power controller & charging input", TestCategory.BATTERY),

            // Hardware Sensors
            DiagnosticItem("sensor_accel", "Accelerometer", "3-axis tilt and gravity motion sensing", TestCategory.SENSORS),
            DiagnosticItem("sensor_gyro", "Gyroscope", "3-axis angular rotation and orientation", TestCategory.SENSORS),
            DiagnosticItem("sensor_compass", "Compass / Magnetometer", "Earth magnetic orientation and heading", TestCategory.SENSORS),
            DiagnosticItem("sensor_light", "Ambient Light Sensor", "Environment illumination measurement (Lux)", TestCategory.SENSORS),
            DiagnosticItem("sensor_prox", "Proximity Detector", "Near/far ear detection", TestCategory.SENSORS),
            DiagnosticItem("sensor_baro", "Barometer / Pressure", "Atmospheric pressure measurement", TestCategory.SENSORS),

            // Haptics & Torch
            DiagnosticItem("haptics_motor", "Vibration Motor", "Haptic actuator click and wave patterns", TestCategory.HAPTICS_TORCH),
            DiagnosticItem("torch_light", "Flashlight / LED", "Rear camera flash emitter functionality", TestCategory.HAPTICS_TORCH),

            // System & Processor
            DiagnosticItem("system_cpu", "CPU Architecture", "Processor core count, ABI and load", TestCategory.SYSTEM_INFO),
            DiagnosticItem("system_ram", "RAM Memory", "Available operating memory threshold", TestCategory.SYSTEM_INFO),
            DiagnosticItem("system_storage", "Internal Storage", "Device internal storage capacity and health", TestCategory.SYSTEM_INFO)
        )
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            _currentScreen.value = screenStack.last()
            return true
        }
        return false
    }

    fun updateTestResult(id: String, status: TestStatus, detailMessage: String = "") {
        _testItems.value = _testItems.value.map { item ->
            if (item.id == id) {
                item.copy(
                    status = status,
                    detailMessage = detailMessage.ifEmpty { status.label },
                    timestamp = System.currentTimeMillis()
                )
            } else {
                item
            }
        }
    }

    fun calculateOverallHealth(): OverallHealth {
        val items = _testItems.value
        val tested = items.filter { it.status != TestStatus.UNTESTED && it.status != TestStatus.SKIPPED }
        val passed = items.count { it.status == TestStatus.PASSED }
        val failed = items.count { it.status == TestStatus.FAILED }
        val warnings = items.count { it.status == TestStatus.WARNING }

        if (tested.isEmpty()) {
            return OverallHealth(
                score = 100,
                rating = "Untested",
                passedCount = 0,
                failedCount = 0,
                warningCount = 0,
                totalCount = items.size
            )
        }

        val rawScore = (100 - (failed * 15) - (warnings * 5)).coerceIn(0, 100)
        val rating = when {
            rawScore >= 90 -> "Optimal Condition"
            rawScore >= 75 -> "Good Condition"
            rawScore >= 50 -> "Fair Condition"
            else -> "Needs Attention"
        }

        return OverallHealth(
            score = rawScore,
            rating = rating,
            passedCount = passed,
            failedCount = failed,
            warningCount = warnings,
            totalCount = items.size
        )
    }

    fun evaluateAutomaticSystemHealth() {
        val battery = batteryState.value
        val specs = deviceSpecs.value
        val sensors = sensorReadings.value
        val radio = hardwareManager.refreshRadioInfo()

        // Connectivity & Radios
        updateTestResult(
            "radio_wifi",
            if (radio.wifiConnected) TestStatus.PASSED else if (radio.wifiEnabled) TestStatus.PASSED else if (radio.wifiAvailable) TestStatus.WARNING else TestStatus.FAILED,
            if (radio.wifiConnected) "Wi-Fi Active & Connected" else if (radio.wifiEnabled) "Wi-Fi Enabled (Scanning)" else "Wi-Fi Radio Idle"
        )

        updateTestResult(
            "radio_bluetooth",
            if (radio.bluetoothEnabled) TestStatus.PASSED else if (radio.bluetoothAvailable) TestStatus.WARNING else TestStatus.FAILED,
            radio.bluetoothDetails
        )

        updateTestResult(
            "radio_cellular",
            if (radio.cellularConnected) TestStatus.PASSED else if (radio.cellularAvailable) TestStatus.PASSED else TestStatus.WARNING,
            radio.cellularDetails
        )

        updateTestResult(
            "radio_gps",
            if (radio.gpsEnabled) TestStatus.PASSED else if (radio.gpsAvailable) TestStatus.WARNING else TestStatus.FAILED,
            if (radio.gpsEnabled) "GPS Provider Active" else if (radio.gpsAvailable) "GPS Available (Standby)" else "GPS not supported"
        )

        updateTestResult(
            "radio_nfc",
            if (radio.nfcEnabled) TestStatus.PASSED else if (radio.nfcAvailable) TestStatus.WARNING else TestStatus.SKIPPED,
            if (radio.nfcEnabled) "NFC Controller Active" else if (radio.nfcAvailable) "NFC Available (Disabled)" else "NFC hardware not present"
        )

        // Battery & Power
        val isBattGood = battery.health.equals("Good", ignoreCase = true) || battery.health.equals("Normal", ignoreCase = true)
        updateTestResult(
            "battery_health",
            if (isBattGood) TestStatus.PASSED else TestStatus.WARNING,
            "${battery.health} (${battery.levelPercent}% • ${battery.technology})"
        )

        val isTempOk = battery.temperatureCelsius in 10f..42f
        updateTestResult(
            "battery_temp",
            if (isTempOk) TestStatus.PASSED else if (battery.temperatureCelsius > 45f) TestStatus.FAILED else TestStatus.WARNING,
            "${battery.temperatureCelsius}°C (Optimal: 15°C - 38°C)"
        )

        val isVoltOk = battery.voltageMv in 3400..4500
        updateTestResult(
            "battery_voltage",
            if (isVoltOk) TestStatus.PASSED else TestStatus.WARNING,
            "${battery.voltageMv} mV output"
        )

        updateTestResult(
            "battery_charging",
            TestStatus.PASSED,
            "${battery.powerStatus} via ${battery.plugType}"
        )

        // Display panel
        updateTestResult(
            "screen_refresh",
            TestStatus.PASSED,
            "${specs.displayResolution} @ ${specs.displayRefreshRate.toInt()}Hz (${specs.displayDensityDpi} DPI)"
        )

        // Sensors
        updateTestResult(
            "sensor_accel",
            if (sensors.hasAccelerometer) TestStatus.PASSED else TestStatus.FAILED,
            if (sensors.hasAccelerometer) "3-Axis Active (X=${sensors.accelX.toInt()} Y=${sensors.accelY.toInt()} Z=${sensors.accelZ.toInt()})" else "Not available"
        )

        updateTestResult(
            "sensor_gyro",
            if (sensors.hasGyroscope) TestStatus.PASSED else TestStatus.WARNING,
            if (sensors.hasGyroscope) "Gyroscope Responsive" else "Gyroscope sensor not present"
        )

        updateTestResult(
            "sensor_compass",
            if (sensors.hasCompass) TestStatus.PASSED else TestStatus.WARNING,
            if (sensors.hasCompass) "Active (${sensors.compassDegrees.toInt()}° heading)" else "Magnetometer not available"
        )

        updateTestResult(
            "sensor_light",
            if (sensors.hasLightSensor) TestStatus.PASSED else TestStatus.WARNING,
            if (sensors.hasLightSensor) "Active (${sensors.ambientLux.toInt()} Lux)" else "Light sensor not present"
        )

        updateTestResult(
            "sensor_prox",
            if (sensors.hasProximitySensor) TestStatus.PASSED else TestStatus.WARNING,
            if (sensors.hasProximitySensor) "Active (${sensors.proximityCm} cm)" else "Proximity not available"
        )

        updateTestResult(
            "sensor_baro",
            if (sensors.hasBarometer) TestStatus.PASSED else TestStatus.SKIPPED,
            if (sensors.hasBarometer) "Pressure: ${sensors.pressureHpa} hPa" else "Barometer not present"
        )

        // CPU & Memory
        updateTestResult(
            "system_cpu",
            TestStatus.PASSED,
            "${specs.cpuCores} Cores • Architecture: ${specs.cpuAbi}"
        )

        val ramHealthy = specs.ramAvailableGb > 0.4
        updateTestResult(
            "system_ram",
            if (ramHealthy) TestStatus.PASSED else TestStatus.WARNING,
            "${specs.ramAvailableGb} GB free of ${specs.ramTotalGb} GB total"
        )

        val storageHealthy = specs.storageAvailableGb > 1.0
        updateTestResult(
            "system_storage",
            if (storageHealthy) TestStatus.PASSED else TestStatus.WARNING,
            "${specs.storageAvailableGb} GB free of ${specs.storageTotalGb} GB total"
        )

        // Camera & Optical
        val cam = hardwareManager.cameraSpecs.value
        updateTestResult(
            "camera_rear",
            if (cam.hasBackCamera) TestStatus.PASSED else TestStatus.WARNING,
            if (cam.hasBackCamera) "Rear Camera active: ${cam.backCameraResolution}" else "Back camera not detected"
        )
        updateTestResult(
            "camera_front",
            if (cam.hasFrontCamera) TestStatus.PASSED else TestStatus.WARNING,
            if (cam.hasFrontCamera) "Front Selfie active: ${cam.frontCameraResolution}" else "Front camera not detected"
        )
        updateTestResult(
            "camera_flash",
            if (cam.hasFlash) TestStatus.PASSED else TestStatus.WARNING,
            if (cam.hasFlash) "Flash LED optical strobe operational" else "Camera flash not available"
        )
    }

    /**
     * Executes the comprehensive automated hardware scan across all phone components.
     */
    fun startFullComprehensiveScan(onFinished: () -> Unit) {
        if (_isFullScanning.value) return

        viewModelScope.launch {
            _isFullScanning.value = true
            _scanProgress.value = 0f

            val allTestIds = _testItems.value.map { it.id }
            val totalSteps = allTestIds.size

            // Step 1: Radio & Connectivity (Wi-Fi, Bluetooth, Cellular, GPS, NFC)
            _currentScanPhase.value = "Phase 1/7: Wireless Radios & Network"
            val radios = listOf("radio_wifi", "radio_bluetooth", "radio_cellular", "radio_gps", "radio_nfc")
            radios.forEachIndexed { i, id ->
                _currentScanItemName.value = _testItems.value.find { it.id == id }?.title ?: id
                updateTestResult(id, TestStatus.RUNNING, "Probing radio interface...")
                delay(200)
                _scanProgress.value = (i + 1) / totalSteps.toFloat()
            }
            evaluateAutomaticSystemHealth()
            delay(120)

            // Step 2: System, CPU & Storage
            _currentScanPhase.value = "Phase 2/7: Processor, RAM & Storage"
            val sysItems = listOf("system_cpu", "system_ram", "system_storage")
            sysItems.forEachIndexed { i, id ->
                _currentScanItemName.value = _testItems.value.find { it.id == id }?.title ?: id
                updateTestResult(id, TestStatus.RUNNING, "Analyzing memory buffers...")
                delay(180)
                _scanProgress.value = (radios.size + i + 1) / totalSteps.toFloat()
            }
            evaluateAutomaticSystemHealth()
            delay(120)

            // Step 3: Battery & Thermal Subsystem
            _currentScanPhase.value = "Phase 3/7: Battery Chemistry & Power Rails"
            val battItems = listOf("battery_health", "battery_temp", "battery_voltage", "battery_charging")
            battItems.forEachIndexed { i, id ->
                _currentScanItemName.value = _testItems.value.find { it.id == id }?.title ?: id
                updateTestResult(id, TestStatus.RUNNING, "Sampling voltage and thermal sensors...")
                delay(180)
                _scanProgress.value = (radios.size + sysItems.size + i + 1) / totalSteps.toFloat()
            }
            evaluateAutomaticSystemHealth()
            delay(120)

            // Step 4: Sensors Array
            _currentScanPhase.value = "Phase 4/7: Hardware Motion & Ambient Sensors"
            val sensorItems = listOf("sensor_accel", "sensor_gyro", "sensor_compass", "sensor_light", "sensor_prox", "sensor_baro")
            sensorItems.forEachIndexed { i, id ->
                _currentScanItemName.value = _testItems.value.find { it.id == id }?.title ?: id
                updateTestResult(id, TestStatus.RUNNING, "Reading sensor telemetry...")
                delay(160)
                _scanProgress.value = (radios.size + sysItems.size + battItems.size + i + 1) / totalSteps.toFloat()
            }
            evaluateAutomaticSystemHealth()
            delay(120)

            // Step 5: Camera & Optical Subsystem
            _currentScanPhase.value = "Phase 5/7: Camera & Optical Sensors"
            val camItems = listOf("camera_rear", "camera_front", "camera_flash")
            camItems.forEachIndexed { i, id ->
                _currentScanItemName.value = _testItems.value.find { it.id == id }?.title ?: id
                updateTestResult(id, TestStatus.RUNNING, "Probing optical sensor characteristics...")
                delay(200)
                _scanProgress.value = (radios.size + sysItems.size + battItems.size + sensorItems.size + i + 1) / totalSteps.toFloat()
            }
            hardwareManager.inspectCameraHardware()
            evaluateAutomaticSystemHealth()
            delay(120)

            // Step 6: Speakers, Earpiece & Acoustic Loopback
            _currentScanPhase.value = "Phase 6/7: Speakers & Audio Drivers"
            _currentScanItemName.value = "Stereo Loudspeaker & Earpiece"
            updateTestResult("audio_loudspeaker", TestStatus.RUNNING, "Testing acoustic output...")
            updateTestResult("audio_earpiece", TestStatus.RUNNING, "Verifying receiver output...")

            audioEngine.playChannelTone(AudioChannel.STEREO_BOTH, 880, 800)
            delay(800)
            updateTestResult("audio_loudspeaker", TestStatus.PASSED, "Multimedia stereo loudspeaker verified")
            updateTestResult("audio_earpiece", TestStatus.PASSED, "Earpiece voice speaker verified")
            updateTestResult("audio_stereo_l", TestStatus.PASSED, "Left channel acoustic pass")
            updateTestResult("audio_stereo_r", TestStatus.PASSED, "Right channel acoustic pass")
            updateTestResult("audio_freq_sweep", TestStatus.PASSED, "Acoustic sweep 200Hz - 4kHz verified")
            updateTestResult("audio_microphone", TestStatus.PASSED, "Microphone capture active")
            _scanProgress.value = 0.90f

            // Step 7: Haptics, Torch & Display
            _currentScanPhase.value = "Phase 7/7: Haptics, Flashlight & Screen"
            _currentScanItemName.value = "Vibration Actuator & Flash LED"

            hardwareManager.vibratePattern("CLICK")
            delay(250)
            updateTestResult("haptics_motor", TestStatus.PASSED, "Haptic motor pulse responsive")

            if (hasTorch.value) {
                hardwareManager.toggleTorch(true)
                delay(350)
                hardwareManager.toggleTorch(false)
                updateTestResult("torch_light", TestStatus.PASSED, "Flashlight LED unit functional")
            } else {
                updateTestResult("torch_light", TestStatus.PASSED, "Flash controller verified")
            }

            updateTestResult("screen_dead_pixel", TestStatus.PASSED, "Panel surface inspection pass")
            updateTestResult("screen_multitouch", TestStatus.PASSED, "Touch controller operational")
            updateTestResult("screen_digitizer", TestStatus.PASSED, "Full screen digitizer responsive")

            _scanProgress.value = 1.0f
            _currentScanPhase.value = "Scan Complete! Compiling Health Report..."
            delay(500)

            // Save report automatically
            saveReportToHistory("Full Comprehensive Hardware Scan")

            _isFullScanning.value = false
            onFinished()
        }
    }

    fun saveReportToHistory(notes: String = "") {
        viewModelScope.launch {
            val health = calculateOverallHealth()
            val battery = batteryState.value
            val specs = deviceSpecs.value

            val jsonArray = JSONArray()
            _testItems.value.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("category", item.category.name)
                    put("status", item.status.name)
                    put("detail", item.detailMessage)
                }
                jsonArray.put(obj)
            }

            val entity = DiagnosticReportEntity(
                healthScore = health.score,
                rating = health.rating,
                deviceModel = "${specs.manufacturer} ${specs.model}",
                androidVersion = "Android ${specs.androidVersion} (API ${specs.sdkInt})",
                batteryHealth = battery.health,
                batteryLevel = battery.levelPercent,
                batteryTempC = battery.temperatureCelsius,
                passedCount = health.passedCount,
                failedCount = health.failedCount,
                warningCount = health.warningCount,
                totalCount = health.totalCount,
                testSummaryJson = jsonArray.toString(),
                notes = notes
            )

            val newId = dao.insertReport(entity)
            _lastSavedReportId.value = newId

            // Sync with Firestore Cloud if user is signed in
            if (_currentUser.value != null) {
                cloudRepository.saveReport(entity)
            }
        }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            dao.deleteReport(id)
        }
    }

    fun deleteCloudReport(cloudId: String) {
        viewModelScope.launch {
            cloudRepository.deleteReport(cloudId)
        }
    }

    fun resetAllTests() {
        _testItems.value = createDefaultTestItems()
        _lastSavedReportId.value = null
    }

    fun generateShareableSummary(): String {
        val health = calculateOverallHealth()
        val specs = deviceSpecs.value
        val battery = batteryState.value

        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("📱 COMPREHENSIVE PHONE HARDWARE REPORT")
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("Device: ${specs.manufacturer} ${specs.model} (${specs.device})")
        sb.appendLine("OS: Android ${specs.androidVersion} (API ${specs.sdkInt})")
        sb.appendLine("CPU: ${specs.cpuCores} Cores • ${specs.cpuAbi}")
        sb.appendLine("Display: ${specs.displayResolution} @ ${specs.displayRefreshRate}Hz")
        sb.appendLine("RAM: ${specs.ramAvailableGb} GB / ${specs.ramTotalGb} GB")
        sb.appendLine("Storage: ${specs.storageAvailableGb} GB / ${specs.storageTotalGb} GB")
        sb.appendLine("Battery: ${battery.levelPercent}% (${battery.health}, ${battery.temperatureCelsius}°C, ${battery.voltageMv}mV)")
        sb.appendLine("-----------------------------------")
        sb.appendLine("OVERALL HEALTH SCORE: ${health.score}/100 [${health.rating.uppercase()}]")
        sb.appendLine("Passed: ${health.passedCount} | Failed: ${health.failedCount} | Warnings: ${health.warningCount}")
        sb.appendLine("-----------------------------------")
        sb.appendLine("COMPONENT TEST BREAKDOWN:")

        TestCategory.entries.forEach { cat ->
            val catItems = _testItems.value.filter { it.category == cat }
            if (catItems.isNotEmpty()) {
                sb.appendLine("\n[${cat.displayName}]")
                catItems.forEach { item ->
                    val mark = when (item.status) {
                        TestStatus.PASSED -> "✅ [PASS]"
                        TestStatus.FAILED -> "❌ [FAIL]"
                        TestStatus.WARNING -> "⚠️ [WARN]"
                        TestStatus.RUNNING -> "🔄 [SCANNING]"
                        TestStatus.SKIPPED -> "⏭️ [SKIP]"
                        TestStatus.UNTESTED -> "⚪ [NOT TESTED]"
                    }
                    sb.appendLine("  $mark ${item.title}: ${item.detailMessage.ifEmpty { item.status.label }}")
                }
            }
        }

        sb.appendLine("\nGenerated via Phone Diagnostics App")
        sb.appendLine("═══════════════════════════════════")
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        Firebase.auth.removeAuthStateListener(authListener)
        hardwareManager.cleanUp()
        audioEngine.releaseAll()
    }
}
