package com.ccs.simplyscannerandroid.data.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Service for importing images from device gallery
 * Handles image selection, processing, and copying to app storage
 */
class GalleryImportService(private val context: Context) {
    
    /**
     * Creates an intent to pick multiple images from gallery
     */
    fun createGalleryPickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        return Intent.createChooser(intent, "Select Images")
    }
    
    /**
     * Creates an intent to pick a single image from gallery
     */
    fun createSingleImagePickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        return Intent.createChooser(intent, "Select Image")
    }
    
    /**
     * Processes the result from gallery picker
     * @param data The intent data from gallery picker
     * @return List of imported image file paths
     */
    suspend fun processGalleryResult(data: Intent?): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val importedFiles = mutableListOf<String>()
                
                data?.let { intent ->
                    // Handle multiple images
                    intent.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            val uri = clipData.getItemAt(i).uri
                            copyImageToAppStorage(uri)?.let { filePath ->
                                importedFiles.add(filePath)
                            }
                        }
                    } ?: run {
                        // Handle single image
                        intent.data?.let { uri ->
                            copyImageToAppStorage(uri)?.let { filePath ->
                                importedFiles.add(filePath)
                            }
                        }
                    }
                }
                
                if (importedFiles.isEmpty()) {
                    Result.failure(Exception("No images were imported"))
                } else {
                    Result.success(importedFiles)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Copies an image from gallery URI to app's internal storage
     * @param uri The URI of the image to copy
     * @return The file path of the copied image, or null if failed
     */
    private suspend fun copyImageToAppStorage(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                
                inputStream?.use { input ->
                    // Create unique filename
                    val fileName = "imported_${UUID.randomUUID()}.jpg"
                    val tempDir = File(context.cacheDir, "gallery_imports")
                    
                    // Create directory if it doesn't exist
                    if (!tempDir.exists()) {
                        tempDir.mkdirs()
                    }
                    
                    val outputFile = File(tempDir, fileName)
                    
                    // Copy the image
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                    
                    outputFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Gets image metadata from URI
     * @param uri The URI of the image
     * @return ImageMetadata object with image information
     */
    suspend fun getImageMetadata(uri: Uri): ImageMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.SIZE,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.WIDTH,
                        MediaStore.Images.Media.HEIGHT
                    ),
                    null,
                    null,
                    null
                )
                
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        val size = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                        val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                        val width = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                        val height = it.getInt(it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                        
                        ImageMetadata(
                            displayName = displayName,
                            size = size,
                            dateAdded = dateAdded * 1000, // Convert to milliseconds
                            width = width,
                            height = height
                        )
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Validates if the selected image is suitable for import
     * @param uri The URI of the image to validate
     * @return True if image is valid for import
     */
    suspend fun validateImage(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                
                inputStream?.use { stream ->
                    // Check if we can read the image
                    val bytes = ByteArray(1024)
                    stream.read(bytes) > 0
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Cleans up temporary imported files
     */
    fun cleanupTempFiles() {
        try {
            val tempDir = File(context.cacheDir, "gallery_imports")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Creates a file provider URI for sharing
     * @param file The file to create URI for
     * @return URI for the file
     */
    fun createFileProviderUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "com.ccs.simplyscannerandroid.fileprovider",
            file
        )
    }
}

/**
 * Data class to hold image metadata
 */
data class ImageMetadata(
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int
)

/**
 * Result data class for gallery import operation
 */
data class GalleryImportResult(
    val success: Boolean,
    val importedFiles: List<String> = emptyList(),
    val error: String? = null
)