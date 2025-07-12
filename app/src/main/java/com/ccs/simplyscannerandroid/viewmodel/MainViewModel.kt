package com.ccs.simplyscannerandroid.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ccs.simplyscannerandroid.data.MockData
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.service.GalleryImportService
import com.ccs.simplyscannerandroid.data.service.ImageProcessingService
import com.ccs.simplyscannerandroid.data.service.ScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val scanService = ScanService(application.applicationContext)
    private val galleryImportService = GalleryImportService(application.applicationContext)
    private val imageProcessingService = ImageProcessingService(application.applicationContext)
    
    // Date formatter for imported document display names following iOS pattern
    private val documentNameFormatter = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val _scanItems = MutableStateFlow<List<ScanItem>>(emptyList())
    val scanItems: StateFlow<List<ScanItem>> = _scanItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _galleryImportProgress = MutableStateFlow(false)
    val galleryImportProgress: StateFlow<Boolean> = _galleryImportProgress.asStateFlow()
    
    private val _showCreateFolderDialog = MutableStateFlow(false)
    val showCreateFolderDialog: StateFlow<Boolean> = _showCreateFolderDialog.asStateFlow()
    
    private val _defaultFolderName = MutableStateFlow("")
    val defaultFolderName: StateFlow<String> = _defaultFolderName.asStateFlow()
    
    init {
        loadScanItems()
    }
    
    private fun loadScanItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Use mock data for testing
                // TODO: Replace with actual ScanService calls when backend is ready
                /*
                val mockItems = MockData.mockScanItems
                _scanItems.value = mockItems.filter { it.deletedDate == null }
                    .sortedByDescending { it.updatedDate }
                */
                // Original implementation (commented out for testing)
                
                scanService.getAllItems()
                    .onSuccess { items ->
                        _scanItems.value = items.filter { it.deletedDate == null }
                            .sortedByDescending { it.updatedDate }
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to load scan items"
                    }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Creates an intent to pick images from gallery
     * @param allowMultiple Whether to allow multiple image selection
     * @return Intent for gallery picker
     */
    fun createGalleryPickerIntent(allowMultiple: Boolean = true): Intent {
        return if (allowMultiple) {
            galleryImportService.createGalleryPickerIntent()
        } else {
            galleryImportService.createSingleImagePickerIntent()
        }
    }
    
    /**
     * Processes imported images from gallery and creates a new document
     * @param galleryResult The intent result from gallery picker
     * @param documentName Optional name for the created document
     */
    fun importImagesFromGallery(
        galleryResult: Intent?,
        documentName: String? = null
    ) {
        viewModelScope.launch {
            _galleryImportProgress.value = true
            _error.value = null
            
            try {
                // Process gallery result to get temporary image paths
                galleryImportService.processGalleryResult(galleryResult)
                    .onSuccess { tempImagePaths ->
                        if (tempImagePaths.isNotEmpty()) {
                            // Create temporary directory for processing
                            val tempDir = File(getApplication<Application>().cacheDir, "processing_${UUID.randomUUID()}")
                            
                            // Process images (resize, rotate, optimize)
                            imageProcessingService.batchProcessImages(tempImagePaths, tempDir)
                                .onSuccess { processedResults ->
                                    // Create new document with processed images
                                    val finalDocumentName = documentName ?: documentNameFormatter.format(Date())
                                    createDocumentFromProcessedImages(finalDocumentName, processedResults)
                                }
                                .onFailure { exception ->
                                    _error.value = "Failed to process images: ${exception.message}"
                                }
                        } else {
                            _error.value = "No images were selected"
                        }
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to import images: ${exception.message}"
                    }
                    
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _galleryImportProgress.value = false
                // Cleanup temporary files
                galleryImportService.cleanupTempFiles()
            }
        }
    }
    
    /**
     * Creates a new document from processed images
     * @param documentName Name for the new document
     * @param processedResults List of processed image results
     */
    private suspend fun createDocumentFromProcessedImages(
        documentName: String,
        processedResults: List<com.ccs.simplyscannerandroid.data.service.ProcessedImageResult>
    ) {
        try {
            // Convert processed image paths to File objects
            val imageFiles = processedResults.map { File(it.outputPath) }
            
            // Use enhanced scan service to create document with proper iOS-style file management
            scanService.createDocumentFromImages(documentName, imageFiles)
                .onSuccess { newDocument ->
                    // Add to current list for immediate UI update
                    val currentItems = _scanItems.value.toMutableList()
                    currentItems.add(0, newDocument) // Add at beginning
                    _scanItems.value = currentItems
                }
                .onFailure { exception ->
                    _error.value = "Failed to create document: ${exception.message}"
                }
            
        } catch (e: Exception) {
            _error.value = "Failed to create document: ${e.message}"
        }
    }
    
    /**
     * Validates selected images before processing
     * @param galleryResult The intent result from gallery picker
     * @return True if all images are valid
     */
    suspend fun validateSelectedImages(galleryResult: Intent?): Boolean {
        return try {
            galleryImportService.processGalleryResult(galleryResult)
                .fold(
                    onSuccess = { tempImagePaths ->
                        tempImagePaths.all { imagePath ->
                            imageProcessingService.validateImageFile(imagePath)
                        }
                    },
                    onFailure = { false }
                )
        } catch (e: Exception) {
            false
        }
    }
    
    fun refreshItems() {
        loadScanItems()
    }
    
    fun createDocument(displayName: String) {
        viewModelScope.launch {
            try {
                scanService.createDocument(displayName)
                    .onSuccess {
                        loadScanItems() // Refresh the list
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to create document"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }
    
    fun createFolder(displayName: String) {
        viewModelScope.launch {
            try {
                scanService.createFolder(displayName)
                    .onSuccess {
                        loadScanItems() // Refresh the list
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to create folder"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }
    
    fun deleteItem(item: ScanItem) {
        viewModelScope.launch {
            try {
                scanService.deleteItem(item.uuid)
                    .onSuccess {
                        loadScanItems() // Refresh the list
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to delete item"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Show create folder dialog with default name generation
     */
    fun showCreateFolderDialog() {
        viewModelScope.launch {
            val defaultName = generateUniqueDefaultFolderName()
            _defaultFolderName.value = defaultName
            _showCreateFolderDialog.value = true
        }
    }
    
    /**
     * Hide create folder dialog
     */
    fun hideCreateFolderDialog() {
        _showCreateFolderDialog.value = false
    }
    
    /**
     * Generate unique default folder name following iOS pattern
     * FR-2: Default folder name with conflict resolution
     */
    private suspend fun generateUniqueDefaultFolderName(): String {
        val currentItems = _scanItems.value
        val baseName = "New Folder"
        
        // Check if base name is available
        if (!isDisplayNameExists(baseName, currentItems)) {
            return baseName
        }
        
        // Find the next available numbered name
        var counter = 1
        while (true) {
            val candidateName = "$baseName ($counter)"
            if (!isDisplayNameExists(candidateName, currentItems)) {
                return candidateName
            }
            counter++
        }
    }
    
    /**
     * Check if a display name already exists in the current directory
     */
    private fun isDisplayNameExists(displayName: String, items: List<ScanItem>): Boolean {
        return items.any { it.displayName.equals(displayName, ignoreCase = true) }
    }
    
    /**
     * Validate folder name according to requirements
     * FR-3: Folder name validation
     */
    private fun validateFolderName(name: String): String? {
        val trimmedName = name.trim()
        
        if (trimmedName.isBlank()) {
            return "Folder name cannot be empty"
        }
        
        if (isDisplayNameExists(trimmedName, _scanItems.value)) {
            return "A folder with this name already exists"
        }
        
        return null // Valid name
    }
    
    /**
     * Create a new folder with validation and error handling
     * FR-1: Folder creation with proper file system structure
     * FR-4: Data persistence and model
     */
    fun createFolderWithName(displayName: String) {
        viewModelScope.launch {
            try {
                val trimmedName = displayName.trim()
                
                // Validate the name
                val validationError = validateFolderName(trimmedName)
                if (validationError != null) {
                    _error.value = validationError
                    return@launch
                }
                
                // Create the folder using ScanService
                scanService.createFolder(trimmedName)
                    .onSuccess { newFolder ->
                        // Add to current list for immediate UI update
                        val currentItems = _scanItems.value.toMutableList()
                        currentItems.add(0, newFolder) // Add at beginning
                        _scanItems.value = currentItems
                        
                        // Close dialog
                        hideCreateFolderDialog()
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to create folder"
                    }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred while creating folder"
            }
        }
    }
}