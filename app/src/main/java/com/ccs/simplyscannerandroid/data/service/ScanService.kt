package com.ccs.simplyscannerandroid.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.model.SortOption
import com.ccs.simplyscannerandroid.data.model.createDocument
import com.ccs.simplyscannerandroid.data.model.createFolder
import com.ccs.simplyscannerandroid.data.model.isDeleted
import com.ccs.simplyscannerandroid.data.model.isDocument
import com.ccs.simplyscannerandroid.data.model.isFolder
import com.ccs.simplyscannerandroid.data.storage.FileOperations
import com.ccs.simplyscannerandroid.data.storage.FileSystemStorage
import com.ccs.simplyscannerandroid.data.storage.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-level service for managing scanned documents and folders
 * Enhanced with iOS-compatible image handling and thumbnail generation
 */
class ScanService(private val context: Context) {
    
    companion object {
        const val DESC_FILE_NAME = "desc.json" // iOS uses desc.ini, Android uses desc.json
        const val THUMB_SIZE = 200 // 200dp thumbnail size (following iOS pattern)
        const val THUMB_PREFIX = "thumb_" // Following iOS naming convention
        const val IMAGE_EXTENSION = ".jpg" // Using JPEG for better compression
    }
    
    private val storage = FileSystemStorage(context)
    private val fileOperations = FileOperations(context)
    
    // Image cache with 50MB memory limit (following iOS pattern)
    private val imageCache = object : LruCache<String, Bitmap>(50 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    // Date formatter following iOS pattern: yyyy-MM-dd_HH_mm_ss.SSS
    private val directoryDateFormatter = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Creates a new document from imported images (following iOS createScan pattern)
     */
    suspend fun createDocumentFromImages(
        displayName: String,
        images: List<File>
    ): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            // Create document using existing pattern
            val documentResult = createDocument(displayName)
            documentResult.fold(
                onSuccess = { document ->
                    // Process and add images
                    val imageFilenames = mutableListOf<String>()
                    images.forEachIndexed { index, imageFile ->
                        val timestamp = directoryDateFormatter.format(Date(document.createdDate))
                        val imageFilename = "$timestamp-$index$IMAGE_EXTENSION"
                        
                        // Get document directory using new date-based naming
                        val documentDir = storage.getItemDirectory(document)
                        val targetFile = File(documentDir, imageFilename)
                        
                        // Process and save image
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        if (bitmap != null) {
                            // Save original image with JPEG compression
                            FileOutputStream(targetFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            
                            // Generate and save thumbnail (following iOS pattern)
                            generateThumbnail(bitmap, File(documentDir, "$THUMB_PREFIX$imageFilename"))
                            
                            imageFilenames.add(imageFilename)
                            
                            // Cache original image
                            val cacheKey = "${document.uuid}-$imageFilename"
                            imageCache.put(cacheKey, bitmap)
                        }
                    }
                    
                    // Update document with image order
                    val finalDocument = document.copy(order = imageFilenames)
                    storage.saveItemMetadata(finalDocument)
                    
                    Result.success(finalDocument)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generates thumbnail following iOS pattern (scaleAspectFill with center crop)
     */
    private fun generateThumbnail(originalBitmap: Bitmap, thumbnailFile: File) {
        try {
            val density = context.resources.displayMetrics.density
            val thumbSizePx = (THUMB_SIZE * density).toInt()
            
            // Calculate scale to fill thumbnail size (scaleAspectFill equivalent)
            val scale = maxOf(
                thumbSizePx.toFloat() / originalBitmap.width,
                thumbSizePx.toFloat() / originalBitmap.height
            )
            
            val scaledWidth = (originalBitmap.width * scale).toInt()
            val scaledHeight = (originalBitmap.height * scale).toInt()
            
            // Scale bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            
            // Crop to center (scaleAspectFill)
            val x = maxOf(0, (scaledWidth - thumbSizePx) / 2)
            val y = maxOf(0, (scaledHeight - thumbSizePx) / 2)
            val width = minOf(thumbSizePx, scaledWidth)
            val height = minOf(thumbSizePx, scaledHeight)
            
            val thumbnail = Bitmap.createBitmap(scaledBitmap, x, y, width, height)
            
            // Ensure parent directory exists
            thumbnailFile.parentFile?.mkdirs()
            
            // Save thumbnail
            FileOutputStream(thumbnailFile).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            // Clean up
            if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
            thumbnail.recycle()
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }
    
    /**
     * Gets original image for a scan item page (following iOS getImage pattern)
     */
    suspend fun getImage(item: ScanItem, pageIndex: Int): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            if (pageIndex >= item.order.size) {
                return@withContext Result.failure(IndexOutOfBoundsException("Page index out of range"))
            }
            
            val filename = item.order[pageIndex]
            val cacheKey = "${item.uuid}-$filename"
            
            // Check cache first (following iOS caching pattern)
            imageCache.get(cacheKey)?.let { cachedBitmap ->
                return@withContext Result.success(cachedBitmap)
            }
            
            // Load from file
            val imageFile = storage.getPageFile(item, filename)
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) {
                    imageCache.put(cacheKey, bitmap)
                    Result.success(bitmap)
                } else {
                    Result.failure(Exception("Failed to decode image"))
                }
            } else {
                Result.failure(Exception("Image file not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets thumbnail image for a scan item page (following iOS getThumbImage pattern)
     */
    suspend fun getThumbnail(item: ScanItem, pageIndex: Int): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            if (pageIndex >= item.order.size) {
                return@withContext Result.failure(IndexOutOfBoundsException("Page index out of range"))
            }
            
            val filename = item.order[pageIndex]
            val thumbFilename = "$THUMB_PREFIX$filename"
            val thumbCacheKey = "${item.uuid}-$thumbFilename"
            
            // Check cache first
            imageCache.get(thumbCacheKey)?.let { cachedThumb ->
                return@withContext Result.success(cachedThumb)
            }
            
            // Check if thumbnail file exists
            val thumbFile = storage.getPageFile(item, thumbFilename)
            if (thumbFile.exists()) {
                val thumbnail = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (thumbnail != null) {
                    imageCache.put(thumbCacheKey, thumbnail)
                    return@withContext Result.success(thumbnail)
                }
            }
            
            // Generate thumbnail from original image if not found (lazy generation like iOS)
            getImage(item, pageIndex).fold(
                onSuccess = { originalBitmap ->
                    generateThumbnail(originalBitmap, thumbFile)
                    val thumbnail = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (thumbnail != null) {
                        imageCache.put(thumbCacheKey, thumbnail)
                        Result.success(thumbnail)
                    } else {
                        Result.failure(Exception("Failed to generate thumbnail"))
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clears image cache (following iOS pattern)
     */
    fun clearImageCache() {
        imageCache.evictAll()
    }
    
    /**
     * Create a new document
     */
    suspend fun createDocument(displayName: String): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = com.ccs.simplyscannerandroid.data.model.createDocument(displayName)
            storage.saveItemMetadata(item)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new folder
     */
    suspend fun createFolder(displayName: String): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = com.ccs.simplyscannerandroid.data.model.createFolder(displayName)
            storage.saveItemMetadata(item)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all items (documents and folders)
     */
    suspend fun getAllItems(): Result<List<ScanItem>> = withContext(Dispatchers.IO) {
        try {
            val uuids = storage.listItemDirectories()
            val items = uuids.mapNotNull { uuid ->
                try {
                    storage.loadItemMetadata(uuid)
                } catch (e: Exception) {
                    null // Skip corrupted items
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all active items (not deleted)
     */
    suspend fun getActiveItems(): Result<List<ScanItem>> = withContext(Dispatchers.IO) {
        try {
            val allItemsResult = getAllItems()
            allItemsResult.fold(
                onSuccess = { items ->
                    val activeItems = items.filter { !it.isDeleted }
                    Result.success(activeItems)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get item size in bytes
     */
    private fun getItemSize(uuid: String): Long {
        return try {
            storage.getItemDirectorySize(uuid)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get items sorted by specified option
     */
    suspend fun getItemsSorted(sortOption: SortOption): Result<List<ScanItem>> = withContext(Dispatchers.IO) {
        try {
            val activeItemsResult = getActiveItems()
            activeItemsResult.fold(
                onSuccess = { items ->
                    val sortedItems = when (sortOption) {
                        SortOption.NAME_ASC -> items.sortedBy { it.displayName.lowercase() }
                        SortOption.NAME_DESC -> items.sortedByDescending { it.displayName.lowercase() }
                        SortOption.DATE_ASC -> items.sortedBy { it.createdDate }
                        SortOption.DATE_DESC -> items.sortedByDescending { it.createdDate }
                        SortOption.SIZE_ASC -> items.sortedBy { getItemSize(it.uuid) }
                        SortOption.SIZE_DESC -> items.sortedByDescending { getItemSize(it.uuid) }
                    }
                    Result.success(sortedItems)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get documents only (filtered)
     */
    suspend fun getDocuments(): Result<List<ScanItem>> = withContext(Dispatchers.IO) {
        try {
            val activeItemsResult = getActiveItems()
            activeItemsResult.fold(
                onSuccess = { items ->
                    val documents = items.filter { it.isDocument }
                    Result.success(documents)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get folders only (filtered)
     */
    suspend fun getFolders(): Result<List<ScanItem>> = withContext(Dispatchers.IO) {
        try {
            val activeItemsResult = getActiveItems()
            activeItemsResult.fold(
                onSuccess = { items ->
                    val folders = items.filter { it.isFolder }
                    Result.success(folders)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get item by UUID
     */
    suspend fun getItem(uuid: String): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(uuid)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update item metadata
     */
    suspend fun updateItem(item: ScanItem): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val updatedItem = item.copy(updatedDate = System.currentTimeMillis())
            storage.saveItemMetadata(updatedItem)
            Result.success(updatedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete item (mark as deleted)
     */
    suspend fun deleteItem(uuid: String): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(uuid)
            val deletedItem = item.copy(
                deletedDate = System.currentTimeMillis(),
                updatedDate = System.currentTimeMillis()
            )
            storage.saveItemMetadata(deletedItem)
            Result.success(deletedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Permanently delete item and all its files
     */
    suspend fun permanentlyDeleteItem(uuid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = storage.deleteItemDirectory(uuid)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Restore deleted item
     */
    suspend fun restoreItem(uuid: String): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(uuid)
            val restoredItem = item.copy(
                deletedDate = null,
                updatedDate = System.currentTimeMillis()
            )
            storage.saveItemMetadata(restoredItem)
            Result.success(restoredItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add page to document from URI
     */
    suspend fun addPageToDocument(
        documentUuid: String,
        imageUri: Uri,
        pageFilename: String? = null
    ): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(documentUuid)
            if (item.isFolder) {
                return@withContext Result.failure(IllegalArgumentException("Cannot add pages to folder"))
            }
            
            val filename = pageFilename ?: fileOperations.generatePageFilename()
            
            // Copy image file to document directory
            fileOperations.copyImageToItemDirectory(imageUri, documentUuid, filename, storage)
            
            // Update item metadata
            val updatedItem = item.copy(
                order = item.order + filename,
                updatedDate = System.currentTimeMillis()
            )
            storage.saveItemMetadata(updatedItem)
            
            Result.success(updatedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove page from document
     */
    suspend fun removePageFromDocument(
        documentUuid: String,
        pageFilename: String
    ): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(documentUuid)
            if (item.isFolder) {
                return@withContext Result.failure(IllegalArgumentException("Cannot remove pages from folder"))
            }
            
            // Delete the page file
            fileOperations.deletePageFile(documentUuid, pageFilename, storage)
            
            // Update item metadata
            val updatedItem = item.copy(
                order = item.order.filter { it != pageFilename },
                updatedDate = System.currentTimeMillis()
            )
            storage.saveItemMetadata(updatedItem)
            
            Result.success(updatedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reorder pages in document
     */
    suspend fun reorderPagesInDocument(
        documentUuid: String,
        newOrder: List<String>
    ): Result<ScanItem> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(documentUuid)
            if (item.isFolder) {
                return@withContext Result.failure(IllegalArgumentException("Cannot reorder pages in folder"))
            }
            
            // Use FileOperations to reorder pages
            val updatedItem = fileOperations.reorderPages(item, newOrder, storage)
            storage.saveItemMetadata(updatedItem)
            
            Result.success(updatedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get URI for page file (for sharing or viewing)
     */
    suspend fun getPageUri(documentUuid: String, pageFilename: String): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            val uri = fileOperations.getPageFileUri(documentUuid, pageFilename, storage)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search items by name
     */
    suspend fun searchItems(query: String): Result<List<ScanItem>> = withContext(Dispatchers.IO) {
        try {
            val activeItemsResult = getActiveItems()
            activeItemsResult.fold(
                onSuccess = { items ->
                    val searchResults = items.filter { item ->
                        item.displayName.contains(query, ignoreCase = true)
                    }
                    Result.success(searchResults)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get storage statistics
     */
    suspend fun getStorageStats(): Result<StorageStats> = withContext(Dispatchers.IO) {
        try {
            val stats = storage.getStorageStats()
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Backup item
     */
    suspend fun backupItem(uuid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupFile = storage.backupItemDirectory(uuid)
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clean up temporary files
     */
    suspend fun cleanupTempFiles(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storage.cleanupTempFiles()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate item integrity
     */
    suspend fun validateItemIntegrity(uuid: String): Result<ValidationResult> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(uuid)
            val issues = mutableListOf<String>()
            
            if (item.isDocument) {
                // Check if all page files exist
                item.order.forEach { pageFilename ->
                    val pageFile = storage.getPageFile(item, pageFilename)
                    if (!pageFile.exists()) {
                        issues.add("Missing page file: $pageFilename")
                    }
                }
            }
            
            val result = ValidationResult(
                isValid = issues.isEmpty(),
                issues = issues
            )
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result of item validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>
)