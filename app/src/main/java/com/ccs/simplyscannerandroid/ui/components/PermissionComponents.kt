package com.ccs.simplyscannerandroid.ui.components

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ccs.simplyscannerandroid.R
import com.ccs.simplyscannerandroid.data.permissions.PermissionManager
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionManager.REQUIRED_PERMISSIONS.toList()
    )
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }
    
    when {
        permissionsState.allPermissionsGranted -> {
            // All permissions granted, trigger callback
            LaunchedEffect(Unit) {
                onPermissionsGranted()
            }
        }
        
        permissionsState.shouldShowRationale -> {
            // Show rationale UI
            PermissionRationaleContent(
                onRequestPermissions = {
                    permissionsState.launchMultiplePermissionRequest()
                },
                permissionsState = permissionsState,
                modifier = modifier
            )
        }
        
        else -> {
            // First time requesting permissions
            PermissionRequestContent(
                onRequestPermissions = {
                    permissionsState.launchMultiplePermissionRequest()
                },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestContent(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_request_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.permission_request_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.permission_request_grant))
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRationaleContent(
    onRequestPermissions: () -> Unit,
    permissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_rationale_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Show specific permission rationales
            val deniedPermissions = permissionsState.revokedPermissions
            deniedPermissions.forEach { permissionState ->
                PermissionRationaleItem(
                    permission = permissionState.permission,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.permission_rationale_grant))
            }
        }
    }
}

@Composable
private fun PermissionRationaleItem(
    permission: String,
    modifier: Modifier = Modifier
) {
    val rationaleText = remember(permission) {
        PermissionManager.getPermissionRationale(permission)
    }
    
    val permissionTitle = remember(permission) {
        when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.READ_MEDIA_IMAGES -> "Media Access"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage Write"
            else -> "Permission"
        }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = permissionTitle,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = rationaleText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionWrapper(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit = {
        PermissionDeniedContent()
    },
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )
    
    when {
        cameraPermissionState.status.isGranted -> {
            onPermissionGranted()
        }
        
        cameraPermissionState.status.shouldShowRationale -> {
            CameraPermissionRationaleContent(
                onRequestPermission = {
                    cameraPermissionState.launchPermissionRequest()
                },
                modifier = modifier
            )
        }
        
        else -> {
            CameraPermissionRequestContent(
                onRequestPermission = {
                    cameraPermissionState.launchPermissionRequest()
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CameraPermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.camera_permission_required),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.camera_permission_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.camera_permission_grant))
        }
    }
}

@Composable
private fun CameraPermissionRationaleContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.camera_permission_rationale_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.camera_permission_rationale_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.camera_permission_grant))
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.permission_denied_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.permission_denied_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}