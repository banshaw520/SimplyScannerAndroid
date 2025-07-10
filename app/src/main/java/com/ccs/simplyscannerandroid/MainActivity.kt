package com.ccs.simplyscannerandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccs.simplyscannerandroid.data.permissions.PermissionManager
import com.ccs.simplyscannerandroid.ui.components.PermissionRequestScreen
import com.ccs.simplyscannerandroid.ui.screens.MainScreen
import com.ccs.simplyscannerandroid.ui.theme.SimplyScannerAndroidTheme
import com.ccs.simplyscannerandroid.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimplyScannerAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
private fun MainApp() {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    
    // Check permissions on composition
    LaunchedEffect(Unit) {
        hasPermissions = PermissionManager.hasAllRequiredPermissions(context)
    }
    
    if (hasPermissions) {
        // Main app content
        val viewModel: MainViewModel = viewModel()
        val scanItems by viewModel.scanItems.collectAsState()
        
        MainScreen(
            scanItems = scanItems,
            onScanButtonClick = {
                // TODO: Navigate to camera screen
            },
            onItemClick = { item ->
                // TODO: Navigate to item details
            },
            onSearchClick = {
                // TODO: Implement search functionality
            },
            onImportClick = {
                // TODO: Implement import from gallery
            },
            onNewFolderClick = {
                // TODO: Implement create new folder
            },
            onSortClick = {
                // TODO: Implement sort functionality
            },
            onViewModeClick = {
                // TODO: Implement view mode toggle
            },
            onSelectClick = {
                // TODO: Implement selection mode
            },
            onMenuClick = {
                // TODO: Show settings/menu
            }
        )
    } else {
        // Permission request screen
        PermissionRequestScreen(
            onPermissionsGranted = {
                hasPermissions = true
            }
        )
    }
}