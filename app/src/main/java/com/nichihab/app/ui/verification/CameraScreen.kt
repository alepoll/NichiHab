package com.nichihab.app.ui.verification

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nichihab.app.ui.onboarding.NeonBlue
import com.nichihab.app.ui.onboarding.PulsingDot
import com.nichihab.app.util.LocalStrings
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onVerificationComplete: (Boolean, String) -> Unit,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Navigate on result
    LaunchedEffect(state.result) {
        state.result?.let { result ->
            kotlinx.coroutines.delay(300)
            onVerificationComplete(result.isVerified, result.reason)
        }
    }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            permissionDeniedPermanently = true
        }
        permissionRequested = true
    }

    // Check permission on launch and on resume (after returning from Settings)
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasCameraPermission) {
        CameraPermissionRequest(
            onNavigateBack = onNavigateBack,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            permanentlyDenied = permissionDeniedPermanently && permissionRequested
        )
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Camera facing state: false = back camera, true = front camera
    var useFrontCamera by remember { mutableStateOf(false) }
    // Store camera provider for rebinding
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Bind camera function
    fun bindCamera(provider: ProcessCameraProvider, preview: PreviewView) {
        val cameraPreview = Preview.Builder().build().also {
            it.setSurfaceProvider(preview.surfaceProvider)
        }
        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                cameraPreview,
                imageCapture
            )
        } catch (e: Exception) { /* Camera bind failed */ }
    }

    // Rebind camera when toggle changes
    LaunchedEffect(useFrontCamera) {
        val cp = cameraProvider
        val pv = previewView
        if (cp != null && pv != null) {
            bindCamera(cp, pv)
        }
    }

    // Rebind camera when clearing captured photo for retry
    LaunchedEffect(state.capturedBitmap) {
        if (state.capturedBitmap == null) {
            val cp = cameraProvider
            val pv = previewView
            if (cp != null && pv != null) {
                bindCamera(cp, pv)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    previewView = this

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        bindCamera(provider, this)
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show static preview if photo is captured to prevent live camera movement
        state.capturedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = s.get("captured_photo"),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Subtle guide overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, s.get("back"), tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.taskEmoji} ${state.taskTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                // Show contextual AI photo prompt
                val promptText = state.photoPrompt?.instruction ?: LocalStrings.current.get("take_photo_verify")
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                state.photoPrompt?.hint?.let { hint ->
                    Text(
                        text = "💡 $hint",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            // Camera flip button
            IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = s.get("switch_camera"),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (state.attempts > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${state.attempts}/${state.maxAttempts}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Processing indicator
            AnimatedVisibility(visible = state.isProcessing, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(LocalStrings.current.get("analyzing_photo"), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error display
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error, modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Capture button
            if (!state.isProcessing && !state.isInCooldown && !state.isCapturing) {
                IconButton(
                    onClick = {
                        viewModel.onCaptureStarted()
                        imageCapture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    val bitmap = com.nichihab.app.util.ImageUtil.byteArrayToBitmap(bytes)
                                    image.close()
                                    if (bitmap != null) {
                                        viewModel.onPhotoCaptured(bitmap)
                                    } else {
                                        viewModel.resetForRetry()
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    viewModel.resetForRetry()
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Camera, "Capture", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            } else if (state.isCapturing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(72.dp))
            }

            // Cooldown
            if (state.isInCooldown) {
                Text(
                    text = "${LocalStrings.current.get("cooldown_label")} ${state.cooldownSeconds / 60}:${"%02d".format(state.cooldownSeconds % 60)}",
                    color = Color.White, style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(LocalStrings.current.get("max_attempts_wait"), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }

        // Dark warning overlay
        AnimatedVisibility(visible = state.isTooDark, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(LocalStrings.current.get("too_dark"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(LocalStrings.current.get("too_dark_desc"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.dismissDarkWarning() }) { Text(LocalStrings.current.get("retake")) }
                            Button(onClick = { viewModel.retryWithDarkPhoto() }) { Text(LocalStrings.current.get("send_anyway")) }
                        }
                    }
                }
            }
        }

        // Blur warning overlay
        AnimatedVisibility(visible = state.isBlurry, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(LocalStrings.current.get("too_blurry"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(LocalStrings.current.get("too_blurry_desc"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.dismissBlurWarning() }) { Text(LocalStrings.current.get("retake")) }
                            Button(onClick = { viewModel.retryWithBlurryPhoto() }) { Text(LocalStrings.current.get("send_anyway")) }
                        }
                    }
                }
            }
        }

        // Offline fallback
        AnimatedVisibility(visible = state.isOffline, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(32.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(LocalStrings.current.get("no_internet"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(LocalStrings.current.get("no_internet_desc"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        // Puente Comercial: Honor System bypass removed
                        TextButton(onClick = { viewModel.resetForRetry() }) { Text(LocalStrings.current.get("try_again")) }
                    }
                }
            }
        }
    }
}

/**
 * Camera permission request screen with runtime permission launcher
 * and deep-link to app settings for permanently denied cases.
 */
@Composable
private fun CameraPermissionRequest(
    onNavigateBack: () -> Unit,
    onRequestPermission: () -> Unit,
    permanentlyDenied: Boolean
) {
    val s = LocalStrings.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(NeonBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = NeonBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = s.get("camera_permission_required"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = s.get("camera_permission_desc"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Visual guide hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonBlue.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulsingDot(size = 10.dp)
                Text(
                    text = if (permanentlyDenied) s.get("camera_perm_settings_guide")
                           else s.get("camera_perm_tap_guide"),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (permanentlyDenied) {
                // Deep-link to app settings
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.get("camera_perm_open_settings"))
                }
            } else {
                // Runtime permission request
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.get("camera_perm_grant_btn"))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateBack) {
                Text(s.get("go_back"))
            }
        }
    }
}
