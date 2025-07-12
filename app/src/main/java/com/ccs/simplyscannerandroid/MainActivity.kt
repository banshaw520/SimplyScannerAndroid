package com.ccs.simplyscannerandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccs.simplyscannerandroid.data.permissions.PermissionManager
import com.ccs.simplyscannerandroid.ui.components.CreateFolderDialog
import com.ccs.simplyscannerandroid.ui.components.PermissionRequestScreen
import com.ccs.simplyscannerandroid.ui.screens.MainScreen
import com.ccs.simplyscannerandroid.ui.theme.SimplyScannerAndroidTheme
import com.ccs.simplyscannerandroid.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    
    // Activity result launcher for gallery import
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                // Process the selected images
                viewModel.importImagesFromGallery(data)
            } else {
                Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
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
    
    @Composable
    private fun MainApp() {
        val context = LocalContext.current
        var hasPermissions by remember { mutableStateOf(false) }
        
        // Initialize ViewModel
        viewModel = viewModel<MainViewModel>()
        
        // Check permissions on composition
        LaunchedEffect(Unit) {
            hasPermissions = PermissionManager.hasAllRequiredPermissions(context)
        }
        
        // Observe gallery import progress and errors
        val galleryImportProgress by viewModel.galleryImportProgress.collectAsState()
        val error by viewModel.error.collectAsState()
        
        // Observe create folder dialog state
        val showCreateFolderDialog by viewModel.showCreateFolderDialog.collectAsState()
        val defaultFolderName by viewModel.defaultFolderName.collectAsState()
        
        // Show error messages
        LaunchedEffect(error) {
            error?.let { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        // Show import progress
        LaunchedEffect(galleryImportProgress) {
            if (galleryImportProgress) {
                Toast.makeText(context, "Processing imported images...", Toast.LENGTH_SHORT).show()
            }
        }
        
        if (hasPermissions) {
            // Main app content
            val scanItems by viewModel.scanItems.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            
            MainScreen(
                scanItems = scanItems,
                isLoading = isLoading,
                isImporting = galleryImportProgress,
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
                    // Launch gallery picker
                    launchGalleryImport()
                },
                onNewFolderClick = {
                    viewModel.showCreateFolderDialog()
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
            
            // Create Folder Dialog
            CreateFolderDialog(
                isVisible = showCreateFolderDialog,
                defaultName = defaultFolderName,
                onDismiss = {
                    viewModel.hideCreateFolderDialog()
                },
                onConfirm = { folderName ->
                    viewModel.createFolderWithName(folderName)
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
    
    /**
     * Launches the gallery import process
     */
    private fun launchGalleryImport() {
        try {
            val intent = viewModel.createGalleryPickerIntent(allowMultiple = true)
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}