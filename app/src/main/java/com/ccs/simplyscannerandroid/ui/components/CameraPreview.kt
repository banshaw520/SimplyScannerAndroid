package com.ccs.simplyscannerandroid.ui.components

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Camera preview composable using CameraX
 */
@Composable
fun CameraPreview(
    onSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                // Set up the preview view
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                
                // Provide surface provider when ready
                post {
                    onSurfaceProviderReady(surfaceProvider)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Camera preview with lifecycle management
 */
@Composable
fun LifecycleAwareCameraPreview(
    onSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        onDispose {
            // Clean up resources when leaving composition
        }
    }
    
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                
                // Provide surface provider when ready
                post {
                    onSurfaceProviderReady(surfaceProvider)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}