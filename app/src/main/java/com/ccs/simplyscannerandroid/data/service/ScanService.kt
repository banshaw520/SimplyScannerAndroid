package com.ccs.simplyscannerandroid.data.service

import android.content.Context
import android.net.Uri
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
import java.io.IOException

/**
 * High-level service for managing scanned documents and folders
 * This service provides a clean API for the rest of the application
 */
class ScanService(context: Context) {
    
    private val storage = FileSystemStorage(context)
    private val fileOperations = FileOperations(context)
    
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
     * Validate item integrity
     */
    suspend fun validateItemIntegrity(uuid: String): Result<ValidationResult> = withContext(Dispatchers.IO) {
        try {
            val item = storage.loadItemMetadata(uuid)
            val issues = mutableListOf<String>()
            
            if (item.isDocument) {
                // Check if all page files exist
                item.order.forEach { pageFilename ->
                    val pageFile = storage.getPageFile(uuid, pageFilename)
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