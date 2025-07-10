package com.ccs.simplyscannerandroid.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Manages storage and camera permissions for the application
 */
object PermissionManager {
    
    /**
     * Required permissions for different Android versions
     */
    val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    /**
     * Optional permissions that enhance functionality
     */
    val OPTIONAL_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE // For legacy support
    )
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check camera permission
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }
    
    /**
     * Check storage read permission
     */
    fun hasStorageReadPermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    /**
     * Check storage write permission (legacy)
     */
    fun hasStorageWritePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Not needed for scoped storage
        } else {
            hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Get missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            !hasPermission(context, permission)
        }
    }
    
    /**
     * Get permission rationale message
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera permission is required to scan documents"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage permission is required to import images"
            Manifest.permission.READ_MEDIA_IMAGES -> "Media permission is required to import images"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage permission is required to save documents"
            else -> "This permission is required for app functionality"
        }
    }
    
    /**
     * Check if we should show permission rationale
     */
    fun shouldShowRationale(context: Context, permission: String): Boolean {
        return when (context) {
            is androidx.fragment.app.FragmentActivity -> {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
            }
            is android.app.Activity -> {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
            }
            else -> false
        }
    }
    
    /**
     * Check if device supports scoped storage
     */
    fun isSupportsScoped(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    /**
     * Check if device enforces scoped storage
     */
    fun isEnforcedScoped(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
    
    /**
     * Get storage info for debugging
     */
    fun getStorageInfo(context: Context): StorageInfo {
        return StorageInfo(
            sdkVersion = Build.VERSION.SDK_INT,
            supportsScoped = isSupportsScoped(),
            enforcesScoped = isEnforcedScoped(),
            hasCameraPermission = hasCameraPermission(context),
            hasStorageReadPermission = hasStorageReadPermission(context),
            hasStorageWritePermission = hasStorageWritePermission(context),
            missingPermissions = getMissingPermissions(context)
        )
    }
}

/**
 * Storage information data class
 */
data class StorageInfo(
    val sdkVersion: Int,
    val supportsScoped: Boolean,
    val enforcesScoped: Boolean,
    val hasCameraPermission: Boolean,
    val hasStorageReadPermission: Boolean,
    val hasStorageWritePermission: Boolean,
    val missingPermissions: List<String>
)