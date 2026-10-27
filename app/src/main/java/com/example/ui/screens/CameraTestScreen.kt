package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TestStatus
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
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraTestScreen(
    viewModel: DiagnosticViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraSpecs by viewModel.cameraSpecs.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            viewModel.hardwareManager.inspectCameraHardware()
        }
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var captureStatusMessage by remember { mutableStateOf("Tap shutter to capture test photo") }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            DiagnosticTopAppBar(
                title = "Camera & Optical Suite",
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
            // Live Viewfinder / Preview Box
            Surface(
                color = DiagnosticSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (hasCameraPermission) {
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

                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture

                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()

                                    try {
                                        cameraProvider.unbindAll()
                                        val cam = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            capture
                                        )
                                        cameraControl = cam
                                    } catch (e: Exception) {
                                        Log.e("CameraTestScreen", "Camera binding failed", e)
                                    }
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
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture

                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()

                                    try {
                                        cameraProvider.unbindAll()
                                        val cam = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            capture
                                        )
                                        cameraControl = cam
                                        cam.cameraControl.enableTorch(isFlashOn)
                                    } catch (e: Exception) {
                                        Log.e("CameraTestScreen", "Camera re-bind failed", e)
                                    }
                                }, executor)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Camera Overlay Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (lensFacing == CameraSelector.LENS_FACING_BACK) "REAR LENS" else "FRONT SELFIE",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        isFlashOn = !isFlashOn
                                        cameraControl?.cameraControl?.enableTorch(isFlashOn)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("camera_flash_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Flash Toggle",
                                        tint = if (isFlashOn) AmberWarning else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                            CameraSelector.LENS_FACING_FRONT
                                        } else {
                                            CameraSelector.LENS_FACING_BACK
                                        }
                                        isFlashOn = false
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("camera_flip_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlipCameraAndroid,
                                        contentDescription = "Flip Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Shutter Trigger Button
                        IconButton(
                            onClick = {
                                val capture = imageCapture ?: return@IconButton
                                val photoFile = File(context.cacheDir, "camera_test_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                                            capturedBitmap = bmp
                                            captureStatusMessage = "Snapshot captured successfully (${photoFile.length() / 1024} KB)"
                                            viewModel.updateTestResult(
                                                if (lensFacing == CameraSelector.LENS_FACING_BACK) "camera_rear" else "camera_front",
                                                TestStatus.PASSED,
                                                "Image capture verified (${bmp?.width}x${bmp?.height})"
                                            )
                                        }

                                        override fun onError(exc: ImageCaptureException) {
                                            captureStatusMessage = "Capture error: ${exc.message}"
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, CyanAccent, CircleShape)
                                .testTag("camera_shutter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture Test Photo",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Camera Permission Required", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = captureStatusMessage,
                fontSize = 12.sp,
                color = if (capturedBitmap != null) GreenSuccess else TextSecondary,
                fontWeight = FontWeight.Medium
            )

            // Captured Thumbnail Strip
            if (capturedBitmap != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DiagnosticSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Test",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Optical Capture Verified", color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Resolution: ${capturedBitmap?.width} × ${capturedBitmap?.height} px", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Hardware Specifications Card
            Text(
                text = "OPTICAL HARDWARE SENSOR SPECS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = DiagnosticSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    CameraSpecRow("Rear Primary Lens", cameraSpecs.backCameraResolution, cameraSpecs.hasBackCamera)
                    CameraSpecRow("Front Selfie Lens", cameraSpecs.frontCameraResolution, cameraSpecs.hasFrontCamera)
                    CameraSpecRow("High-Power Flash", if (cameraSpecs.hasFlash) "Integrated LED Torch" else "Not Present", cameraSpecs.hasFlash)
                    CameraSpecRow("Total Camera Units", "${cameraSpecs.totalCamerasCount} Physical Sensors", cameraSpecs.totalCamerasCount > 0)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Verification Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateTestResult("camera_rear", TestStatus.PASSED, "Rear camera verified by user")
                        viewModel.updateTestResult("camera_front", TestStatus.PASSED, "Front camera verified by user")
                        viewModel.updateTestResult("camera_flash", TestStatus.PASSED, "Camera flash verified by user")
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("camera_pass_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Cameras Working", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.updateTestResult("camera_rear", TestStatus.WARNING, "Camera issues reported by user")
                        onBack()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("camera_warn_button")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report Defect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CameraSpecRow(label: String, value: String, isAvailable: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isAvailable) GreenSuccess else TextMuted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                color = if (isAvailable) TextPrimary else TextMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}
