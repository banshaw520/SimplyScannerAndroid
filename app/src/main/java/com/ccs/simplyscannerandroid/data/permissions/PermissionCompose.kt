package com.ccs.simplyscannerandroid.data.permissions

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Compose permission handler for app permissions
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: (List<String>) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionManager.REQUIRED_PERMISSIONS.toList()
    )
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        } else {
            val deniedPermissions = permissionsState.revokedPermissions.map { it.permission }
            onPermissionsDenied(deniedPermissions)
        }
    }
    
    content()
}

/**
 * Permission state data class
 */
data class PermissionState(
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
    val isPermanentlyDenied: Boolean
)

/**
 * Get permission state for UI
 */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    
    return remember {
        val hasPermissions = PermissionManager.hasAllRequiredPermissions(context)
        val missingPermissions = PermissionManager.getMissingPermissions(context)
        
        val shouldShowRationale = missingPermissions.any { permission ->
            PermissionManager.shouldShowRationale(context, permission)
        }
        
        PermissionState(
            isGranted = hasPermissions,
            shouldShowRationale = shouldShowRationale,
            isPermanentlyDenied = !hasPermissions && !shouldShowRationale
        )
    }
}

/**
 * Permission result handler
 */
sealed class PermissionResult {
    object Granted : PermissionResult()
    data class Denied(val permissions: List<String>) : PermissionResult()
    data class PermanentlyDenied(val permissions: List<String>) : PermissionResult()
}

/**
 * Handle permission result
 */
@OptIn(ExperimentalPermissionsApi::class)
fun handlePermissionResult(
    permissionsState: MultiplePermissionsState
): PermissionResult {
    return when {
        permissionsState.allPermissionsGranted -> PermissionResult.Granted
        permissionsState.shouldShowRationale -> {
            val deniedPermissions = permissionsState.revokedPermissions.map { it.permission }
            PermissionResult.Denied(deniedPermissions)
        }
        else -> {
            val deniedPermissions = permissionsState.revokedPermissions.map { it.permission }
            PermissionResult.PermanentlyDenied(deniedPermissions)
        }
    }
}