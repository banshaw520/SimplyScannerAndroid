package com.ccs.simplyscannerandroid.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX service for managing camera operations
 */
class CameraService(private val context: Context) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    
    companion object {
        private const val TAG = "CameraService"
        private const val PHOTO_TYPE = "image/jpeg"
    }
    
    /**
     * Initialize CameraX
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            cameraProvider = getCameraProvider()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start camera with preview and image capture
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ): Result<Camera> {
        return try {
            val provider = cameraProvider ?: return Result.failure(
                IllegalStateException("Camera not initialized")
            )
            
            // Create preview use case
            val preview = Preview.Builder()
                .build()
            
            preview.setSurfaceProvider(surfaceProvider)
            
            // Create image capture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Create camera selector
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            
            // Unbind all use cases before rebinding
            provider.unbindAll()
            
            // Bind use cases to camera
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            Result.success(camera!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
            Result.failure(e)
        }
    }
    
    /**
     * Capture photo
     */
    suspend fun capturePhoto(outputFile: File): Result<CaptureResult> {
        return suspendCancellableCoroutine { continuation ->
            val capture = imageCapture ?: run {
                continuation.resumeWithException(
                    IllegalStateException("Image capture not initialized")
                )
                return@suspendCancellableCoroutine
            }
            
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
                .build()
            
            capture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(outputFile)
                        val result = CaptureResult(
                            savedUri = savedUri,
                            file = outputFile,
                            fileSize = outputFile.length(),
                            timestamp = System.currentTimeMillis()
                        )
                        continuation.resume(Result.success(result))
                    }
                    
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        continuation.resume(Result.failure(exception))
                    }
                }
            )
        }
    }
    
    /**
     * Get camera capabilities
     */
    fun getCameraCapabilities(): CameraCapabilities? {
        return camera?.let { camera ->
            val cameraInfo = camera.cameraInfo
            val zoomState = cameraInfo.zoomState.value
            val zoomRange = if (zoomState != null) {
                zoomState.minZoomRatio..zoomState.maxZoomRatio
            } else {
                1f..1f
            }
            
            CameraCapabilities(
                hasFlash = cameraInfo.hasFlashUnit(),
                supportedFlashModes = listOf(
                    ImageCapture.FLASH_MODE_OFF,
                    ImageCapture.FLASH_MODE_ON,
                    ImageCapture.FLASH_MODE_AUTO
                ),
                zoomRange = zoomRange
            )
        }
    }
    
    /**
     * Set flash mode
     */
    fun setFlashMode(flashMode: Int): Result<Unit> {
        return try {
            imageCapture?.flashMode = flashMode
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set flash mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Set zoom ratio
     */
    fun setZoomRatio(ratio: Float): Result<Unit> {
        return try {
            camera?.cameraControl?.setZoomRatio(ratio)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set zoom ratio", e)
            Result.failure(e)
        }
    }
    
    /**
     * Focus on tap
     */
    fun focusOnTap(x: Float, y: Float, width: Int, height: Int): Result<Unit> {
        return try {
            val factory = SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point)
                .addPoint(point, FocusMeteringAction.FLAG_AF)
                .build()
            
            camera?.cameraControl?.startFocusAndMetering(action)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to focus on tap", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop camera
     */
    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            camera = null
            imageCapture = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop camera", e)
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopCamera()
        cameraProvider = null
    }
    
    /**
     * Get available cameras
     */
    fun getAvailableCameras(): List<CameraInfo> {
        return cameraProvider?.availableCameraInfos ?: emptyList()
    }
    
    /**
     * Check if camera is available
     */
    fun isCameraAvailable(lensFacing: Int): Boolean {
        return cameraProvider?.hasCamera(
            CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
        ) ?: false
    }
    
    /**
     * Get camera provider
     */
    private suspend fun getCameraProvider(): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    continuation.resume(provider)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}

/**
 * Camera capture result
 */
data class CaptureResult(
    val savedUri: Uri,
    val file: File,
    val fileSize: Long,
    val timestamp: Long
)

/**
 * Camera capabilities
 */
data class CameraCapabilities(
    val hasFlash: Boolean,
    val supportedFlashModes: List<Int>,
    val zoomRange: ClosedFloatingPointRange<Float>
)

/**
 * Camera state
 */
sealed class CameraState {
    object Idle : CameraState()
    object Initializing : CameraState()
    object Ready : CameraState()
    object Capturing : CameraState()
    data class Error(val exception: Throwable) : CameraState()
}

/**
 * Flash mode extensions
 */
fun Int.toFlashModeString(): String {
    return when (this) {
        ImageCapture.FLASH_MODE_OFF -> "Off"
        ImageCapture.FLASH_MODE_ON -> "On"
        ImageCapture.FLASH_MODE_AUTO -> "Auto"
        else -> "Unknown"
    }
}

/**
 * Lens facing extensions
 */
fun Int.toLensFacingString(): String {
    return when (this) {
        CameraSelector.LENS_FACING_FRONT -> "Front"
        CameraSelector.LENS_FACING_BACK -> "Back"
        else -> "Unknown"
    }
}