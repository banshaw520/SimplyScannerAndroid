package com.ccs.simplyscannerandroid.camera

import android.app.Application
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for camera operations
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {
    
    private val cameraService = CameraService(application.applicationContext)
    
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()
    
    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()
    
    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()
    
    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()
    
    private val _capabilities = MutableStateFlow<CameraCapabilities?>(null)
    val capabilities: StateFlow<CameraCapabilities?> = _capabilities.asStateFlow()
    
    private val _lastCaptureResult = MutableStateFlow<CaptureResult?>(null)
    val lastCaptureResult: StateFlow<CaptureResult?> = _lastCaptureResult.asStateFlow()
    
    companion object {
        private const val TAG = "CameraViewModel"
    }
    
    init {
        initializeCamera()
    }
    
    /**
     * Initialize camera service
     */
    private fun initializeCamera() {
        viewModelScope.launch {
            _cameraState.value = CameraState.Initializing
            
            cameraService.initialize()
                .onSuccess {
                    _cameraState.value = CameraState.Ready
                    Log.d(TAG, "Camera initialized successfully")
                }
                .onFailure { exception ->
                    _cameraState.value = CameraState.Error(exception)
                    Log.e(TAG, "Camera initialization failed", exception)
                }
        }
    }
    
    /**
     * Start camera with preview
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        viewModelScope.launch {
            if (_cameraState.value != CameraState.Ready) {
                Log.w(TAG, "Camera not ready, current state: ${_cameraState.value}")
                return@launch
            }
            
            cameraService.startCamera(
                lifecycleOwner = lifecycleOwner,
                surfaceProvider = surfaceProvider,
                lensFacing = _lensFacing.value
            ).onSuccess { camera ->
                updateCameraCapabilities()
                Log.d(TAG, "Camera started successfully")
            }.onFailure { exception ->
                _cameraState.value = CameraState.Error(exception)
                Log.e(TAG, "Failed to start camera", exception)
            }
        }
    }
    
    /**
     * Capture photo
     */
    fun capturePhoto(outputFile: File) {
        viewModelScope.launch {
            if (_cameraState.value != CameraState.Ready) {
                Log.w(TAG, "Camera not ready for capture, current state: ${_cameraState.value}")
                return@launch
            }
            
            _cameraState.value = CameraState.Capturing
            
            cameraService.capturePhoto(outputFile)
                .onSuccess { result ->
                    _lastCaptureResult.value = result
                    _cameraState.value = CameraState.Ready
                    Log.d(TAG, "Photo captured successfully: ${result.savedUri}")
                }
                .onFailure { exception ->
                    _cameraState.value = CameraState.Error(exception)
                    Log.e(TAG, "Photo capture failed", exception)
                }
        }
    }
    
    /**
     * Toggle flash mode
     */
    fun toggleFlashMode() {
        val currentMode = _flashMode.value
        val newMode = when (currentMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_OFF
        }
        
        setFlashMode(newMode)
    }
    
    /**
     * Set flash mode
     */
    fun setFlashMode(flashMode: Int) {
        viewModelScope.launch {
            cameraService.setFlashMode(flashMode)
                .onSuccess {
                    _flashMode.value = flashMode
                    Log.d(TAG, "Flash mode set to: ${flashMode.toFlashModeString()}")
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to set flash mode", exception)
                }
        }
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        val newLensFacing = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        
        if (cameraService.isCameraAvailable(newLensFacing)) {
            _lensFacing.value = newLensFacing
            // Camera will be restarted with new lens facing
            Log.d(TAG, "Switched to camera: ${newLensFacing.toLensFacingString()}")
        } else {
            Log.w(TAG, "Camera not available: ${newLensFacing.toLensFacingString()}")
        }
    }
    
    /**
     * Set zoom ratio
     */
    fun setZoomRatio(ratio: Float) {
        viewModelScope.launch {
            val capabilities = _capabilities.value
            if (capabilities != null) {
                val clampedRatio = ratio.coerceIn(capabilities.zoomRange)
                
                cameraService.setZoomRatio(clampedRatio)
                    .onSuccess {
                        _zoomRatio.value = clampedRatio
                        Log.d(TAG, "Zoom ratio set to: $clampedRatio")
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "Failed to set zoom ratio", exception)
                    }
            }
        }
    }
    
    /**
     * Focus on tap
     */
    fun focusOnTap(x: Float, y: Float, width: Int, height: Int) {
        viewModelScope.launch {
            cameraService.focusOnTap(x, y, width, height)
                .onSuccess {
                    Log.d(TAG, "Focus on tap at ($x, $y)")
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to focus on tap", exception)
                }
        }
    }
    
    /**
     * Update camera capabilities
     */
    private fun updateCameraCapabilities() {
        val capabilities = cameraService.getCameraCapabilities()
        _capabilities.value = capabilities
        
        if (capabilities != null) {
            // Reset zoom ratio within new capabilities
            _zoomRatio.value = _zoomRatio.value.coerceIn(capabilities.zoomRange)
            Log.d(TAG, "Camera capabilities updated: $capabilities")
        }
    }
    
    /**
     * Stop camera
     */
    fun stopCamera() {
        cameraService.stopCamera()
        _cameraState.value = CameraState.Ready
        Log.d(TAG, "Camera stopped")
    }
    
    /**
     * Check if front camera is available
     */
    fun isFrontCameraAvailable(): Boolean {
        return cameraService.isCameraAvailable(CameraSelector.LENS_FACING_FRONT)
    }
    
    /**
     * Check if back camera is available
     */
    fun isBackCameraAvailable(): Boolean {
        return cameraService.isCameraAvailable(CameraSelector.LENS_FACING_BACK)
    }
    
    /**
     * Clear last capture result
     */
    fun clearLastCaptureResult() {
        _lastCaptureResult.value = null
    }
    
    /**
     * Reset camera state
     */
    fun resetCameraState() {
        if (_cameraState.value is CameraState.Error) {
            _cameraState.value = CameraState.Ready
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraService.release()
        Log.d(TAG, "Camera service released")
    }
}