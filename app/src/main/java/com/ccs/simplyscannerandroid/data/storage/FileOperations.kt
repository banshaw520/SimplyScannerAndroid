package com.ccs.simplyscannerandroid.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.model.reorderPages
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * File operations utility for handling image files and page management
 */
class FileOperations(private val context: Context) {
    
    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.ccs.simplyscannerandroid.fileprovider"
        private const val IMAGE_QUALITY = 85 // JPEG quality
        private const val MAX_IMAGE_SIZE = 2048 // Maximum width/height in pixels
        
        // Supported image formats
        val SUPPORTED_FORMATS = setOf("jpg", "jpeg", "png", "webp")
    }
    
    /**
     * Copy image from URI to item directory
     */
    @Throws(IOException::class)
    fun copyImageToItemDirectory(
        sourceUri: Uri,
        targetUuid: String,
        targetFilename: String,
        storage: FileSystemStorage
    ): File {
        val targetFile = storage.getPageFile(targetUuid, targetFilename)
        
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                // Load and potentially resize the image
                val bitmap = BitmapFactory.decodeStream(inputStream)
                    ?: throw IOException("Failed to decode image")
                
                val resizedBitmap = resizeImageIfNeeded(bitmap)
                
                // Save to target file
                FileOutputStream(targetFile).use { outputStream ->
                    val format = getCompressionFormat(targetFilename)
                    resizedBitmap.compress(format, IMAGE_QUALITY, outputStream)
                }
                
                // Clean up
                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                bitmap.recycle()
                
                return targetFile
            } ?: throw IOException("Failed to open input stream")
        } catch (e: Exception) {
            // Clean up target file if it was created
            if (targetFile.exists()) {
                targetFile.delete()
            }
            throw IOException("Failed to copy image: ${e.message}", e)
        }
    }
    
    /**
     * Generate unique filename for a new page
     */
    fun generatePageFilename(extension: String = "jpg"): String {
        return "page_${UUID.randomUUID().toString().take(8)}.${extension.lowercase()}"
    }
    
    /**
     * Generate sequential filename for pages
     */
    fun generateSequentialPageFilename(pageNumber: Int, extension: String = "jpg"): String {
        return "page_${pageNumber.toString().padStart(3, '0')}.${extension.lowercase()}"
    }
    
    /**
     * Move page file to new location
     */
    @Throws(IOException::class)
    fun movePageFile(
        fromUuid: String,
        toUuid: String,
        filename: String,
        storage: FileSystemStorage
    ): Boolean {
        val sourceFile = storage.getPageFile(fromUuid, filename)
        val targetFile = storage.getPageFile(toUuid, filename)
        
        if (!sourceFile.exists()) {
            throw IOException("Source file not found: ${sourceFile.absolutePath}")
        }
        
        return try {
            sourceFile.renameTo(targetFile)
        } catch (e: Exception) {
            throw IOException("Failed to move file: ${e.message}", e)
        }
    }
    
    /**
     * Copy page file to new location
     */
    @Throws(IOException::class)
    fun copyPageFile(
        fromUuid: String,
        toUuid: String,
        filename: String,
        storage: FileSystemStorage
    ): Boolean {
        val sourceFile = storage.getPageFile(fromUuid, filename)
        val targetFile = storage.getPageFile(toUuid, filename)
        
        if (!sourceFile.exists()) {
            throw IOException("Source file not found: ${sourceFile.absolutePath}")
        }
        
        return try {
            sourceFile.copyTo(targetFile, overwrite = true)
            true
        } catch (e: Exception) {
            throw IOException("Failed to copy file: ${e.message}", e)
        }
    }
    
    /**
     * Delete page file
     */
    fun deletePageFile(uuid: String, filename: String, storage: FileSystemStorage): Boolean {
        val file = storage.getPageFile(uuid, filename)
        return file.delete()
    }
    
    /**
     * Get URI for sharing or viewing a page file
     */
    fun getPageFileUri(uuid: String, filename: String, storage: FileSystemStorage): Uri? {
        val file = storage.getPageFile(uuid, filename)
        return if (file.exists()) {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        } else null
    }
    
    /**
     * Resize image if it exceeds maximum size
     */
    private fun resizeImageIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }
        
        val scale = minOf(
            MAX_IMAGE_SIZE.toFloat() / width,
            MAX_IMAGE_SIZE.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Get compression format based on filename extension
     */
    private fun getCompressionFormat(filename: String): Bitmap.CompressFormat {
        val extension = filename.substringAfterLast('.').lowercase()
        return when (extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
    }
    
    /**
     * Validate if filename has supported format
     */
    fun isSupportedFormat(filename: String): Boolean {
        val extension = filename.substringAfterLast('.').lowercase()
        return extension in SUPPORTED_FORMATS
    }
    
    /**
     * Get image dimensions without loading full bitmap
     */
    fun getImageDimensions(uuid: String, filename: String, storage: FileSystemStorage): Pair<Int, Int>? {
        val file = storage.getPageFile(uuid, filename)
        if (!file.exists()) return null
        
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Load bitmap from page file
     */
    fun loadPageBitmap(uuid: String, filename: String, storage: FileSystemStorage): Bitmap? {
        val file = storage.getPageFile(uuid, filename)
        return if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * Save bitmap to page file
     */
    @Throws(IOException::class)
    fun saveBitmapToPageFile(
        bitmap: Bitmap,
        uuid: String,
        filename: String,
        storage: FileSystemStorage
    ): File {
        val file = storage.getPageFile(uuid, filename)
        
        try {
            FileOutputStream(file).use { outputStream ->
                val format = getCompressionFormat(filename)
                bitmap.compress(format, IMAGE_QUALITY, outputStream)
            }
            return file
        } catch (e: Exception) {
            throw IOException("Failed to save bitmap: ${e.message}", e)
        }
    }
    
    /**
     * Reorder pages in document by renaming files
     */
    @Throws(IOException::class)
    fun reorderPages(
        item: ScanItem,
        newOrder: List<String>,
        storage: FileSystemStorage
    ): ScanItem {
        if (newOrder.size != item.order.size) {
            throw IllegalArgumentException("New order must have same number of pages")
        }
        
        val tempDir = storage.getTempDirectory()
        val tempFiles = mutableMapOf<String, File>()
        
        try {
            // Move all files to temporary location
            item.order.forEach { filename ->
                val originalFile = storage.getPageFile(item.uuid, filename)
                val tempFile = File(tempDir, "${UUID.randomUUID()}_$filename")
                if (originalFile.exists()) {
                    originalFile.renameTo(tempFile)
                    tempFiles[filename] = tempFile
                }
            }
            
            // Move files back in new order
            newOrder.forEachIndexed { index, filename ->
                val tempFile = tempFiles[filename]
                val newFilename = generateSequentialPageFilename(index + 1, 
                    filename.substringAfterLast('.'))
                val targetFile = storage.getPageFile(item.uuid, newFilename)
                
                tempFile?.renameTo(targetFile)
            }
            
            // Update item with new order
            val updatedOrder = newOrder.mapIndexed { index, filename ->
                generateSequentialPageFilename(index + 1, filename.substringAfterLast('.'))
            }
            
            return item.reorderPages(updatedOrder)
            
        } catch (e: Exception) {
            // Restore original files if something went wrong
            tempFiles.forEach { (original, temp) ->
                val originalFile = storage.getPageFile(item.uuid, original)
                temp.renameTo(originalFile)
            }
            throw IOException("Failed to reorder pages: ${e.message}", e)
        } finally {
            // Clean up any remaining temp files
            tempFiles.values.forEach { it.delete() }
        }
    }
}