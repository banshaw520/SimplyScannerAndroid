package com.ccs.simplyscannerandroid.data.service

import android.content.Context
import android.net.Uri
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.model.SortOption
import com.ccs.simplyscannerandroid.data.model.ViewMode
import com.ccs.simplyscannerandroid.data.model.isDocument
import com.ccs.simplyscannerandroid.data.model.isFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

/**
 * Extension functions and utilities for ScanService
 */

/**
 * Batch operations for ScanService
 */
class ScanServiceBatch(private val scanService: ScanService) {
    
    /**
     * Delete multiple items at once
     */
    suspend fun deleteItems(uuids: List<String>): Result<List<ScanItem>> {
        val results = mutableListOf<ScanItem>()
        val errors = mutableListOf<Exception>()
        
        for (uuid in uuids) {
            scanService.deleteItem(uuid)
                .onSuccess { results.add(it) }
                .onFailure { errors.add(it as Exception) }
        }
        
        return if (errors.isEmpty()) {
            Result.success(results)
        } else {
            Result.failure(ScanServiceException.StorageException(
                "Failed to delete ${errors.size} items out of ${uuids.size}"
            ))
        }
    }
    
    /**
     * Move multiple items (mark as deleted and create copies)
     */
    suspend fun moveItems(uuids: List<String>, targetFolderUuid: String?): Result<List<ScanItem>> {
        // For now, just mark as moved by updating metadata
        // In a full implementation, this would handle folder hierarchies
        val results = mutableListOf<ScanItem>()
        val errors = mutableListOf<Exception>()
        
        for (uuid in uuids) {
            scanService.getItem(uuid)
                .onSuccess { item ->
                    val updatedItem = item.copy(
                        updatedDate = System.currentTimeMillis()
                        // Add folder reference when implementing folder hierarchy
                    )
                    scanService.updateItem(updatedItem)
                        .onSuccess { results.add(it) }
                        .onFailure { errors.add(it as Exception) }
                }
                .onFailure { errors.add(it as Exception) }
        }
        
        return if (errors.isEmpty()) {
            Result.success(results)
        } else {
            Result.failure(ScanServiceException.StorageException(
                "Failed to move ${errors.size} items out of ${uuids.size}"
            ))
        }
    }
    
    /**
     * Validate multiple items
     */
    suspend fun validateItems(uuids: List<String>): Result<Map<String, ValidationResult>> {
        val results = mutableMapOf<String, ValidationResult>()
        val errors = mutableListOf<Exception>()
        
        for (uuid in uuids) {
            scanService.validateItemIntegrity(uuid)
                .onSuccess { results[uuid] = it }
                .onFailure { errors.add(it as Exception) }
        }
        
        return if (errors.isEmpty()) {
            Result.success(results)
        } else {
            Result.failure(ScanServiceException.StorageException(
                "Failed to validate ${errors.size} items"
            ))
        }
    }
}

/**
 * Cache for frequently accessed items
 */
class ScanServiceCache {
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val maxAge = 5 * 60 * 1000L // 5 minutes
    
    data class CacheEntry(
        val item: ScanItem,
        val timestamp: Long
    )
    
    fun get(uuid: String): ScanItem? {
        val entry = cache[uuid]
        return if (entry != null && (System.currentTimeMillis() - entry.timestamp) < maxAge) {
            entry.item
        } else {
            cache.remove(uuid)
            null
        }
    }
    
    fun put(uuid: String, item: ScanItem) {
        cache[uuid] = CacheEntry(item, System.currentTimeMillis())
    }
    
    fun remove(uuid: String) {
        cache.remove(uuid)
    }
    
    fun clear() {
        cache.clear()
    }
    
    fun size(): Int = cache.size
}

/**
 * Enhanced ScanService with additional features
 */
class EnhancedScanService(context: Context) {
    private val scanService = ScanService(context)
    private val cache = ScanServiceCache()
    
    val batch = ScanServiceBatch(scanService)
    
    /**
     * Get item with caching
     */
    suspend fun getItemCached(uuid: String): Result<ScanItem> {
        val cached = cache.get(uuid)
        return if (cached != null) {
            Result.success(cached)
        } else {
            scanService.getItem(uuid).onSuccess { item ->
                cache.put(uuid, item)
            }
        }
    }
    
    /**
     * Update item and invalidate cache
     */
    suspend fun updateItemAndInvalidateCache(item: ScanItem): Result<ScanItem> {
        return scanService.updateItem(item).onSuccess { updatedItem ->
            cache.put(item.uuid, updatedItem)
        }
    }
    
    /**
     * Get items as Flow for reactive UI
     */
    fun getItemsAsFlow(sortOption: SortOption): Flow<List<ScanItem>> = flow {
        val result = scanService.getItemsSorted(sortOption)
        result.onSuccess { items ->
            emit(items)
        }
    }
    
    /**
     * Get filtered items
     */
    suspend fun getFilteredItems(
        includeDocuments: Boolean = true,
        includeFolders: Boolean = true,
        sortOption: SortOption = SortOption.DATE_DESC
    ): Result<List<ScanItem>> {
        return scanService.getItemsSorted(sortOption).map { items ->
            items.filter { item ->
                (includeDocuments && item.isDocument) || 
                (includeFolders && item.isFolder)
            }
        }
    }
    
    /**
     * Get statistics with caching
     */
    suspend fun getStatistics(): Result<ServiceStatistics> {
        return scanService.getActiveItems().map { items ->
            ServiceStatistics(
                totalItems = items.size,
                totalDocuments = items.count { it.isDocument },
                totalFolders = items.count { it.isFolder },
                totalPages = items.filter { it.isDocument }.sumOf { it.order.size },
                cacheSize = cache.size()
            )
        }
    }
    
    /**
     * Delegate other methods to the underlying service
     */
    suspend fun createDocument(displayName: String) = scanService.createDocument(displayName)
    suspend fun createFolder(displayName: String) = scanService.createFolder(displayName)
    suspend fun deleteItem(uuid: String) = scanService.deleteItem(uuid).also { cache.remove(uuid) }
    suspend fun addPageToDocument(documentUuid: String, imageUri: Uri, pageFilename: String? = null) = 
        scanService.addPageToDocument(documentUuid, imageUri, pageFilename).also { cache.remove(documentUuid) }
    suspend fun removePageFromDocument(documentUuid: String, pageFilename: String) = 
        scanService.removePageFromDocument(documentUuid, pageFilename).also { cache.remove(documentUuid) }
    suspend fun reorderPagesInDocument(documentUuid: String, newOrder: List<String>) = 
        scanService.reorderPagesInDocument(documentUuid, newOrder).also { cache.remove(documentUuid) }
    suspend fun getPageUri(documentUuid: String, pageFilename: String) = 
        scanService.getPageUri(documentUuid, pageFilename)
    suspend fun searchItems(query: String) = scanService.searchItems(query)
    suspend fun getStorageStats() = scanService.getStorageStats()
    suspend fun cleanupTempFiles() = scanService.cleanupTempFiles()
    suspend fun validateItemIntegrity(uuid: String) = scanService.validateItemIntegrity(uuid)
}

/**
 * Service statistics
 */
data class ServiceStatistics(
    val totalItems: Int,
    val totalDocuments: Int,
    val totalFolders: Int,
    val totalPages: Int,
    val cacheSize: Int
)

/**
 * UI state for items list
 */
data class ItemsListState(
    val items: List<ScanItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchQuery: String = "",
    val selectedItems: Set<String> = emptySet()
)

/**
 * Extension functions for UI state management
 */
fun ItemsListState.withItems(items: List<ScanItem>): ItemsListState {
    return copy(items = items, isLoading = false, error = null)
}

fun ItemsListState.withLoading(loading: Boolean): ItemsListState {
    return copy(isLoading = loading)
}

fun ItemsListState.withError(error: String): ItemsListState {
    return copy(error = error, isLoading = false)
}

fun ItemsListState.withSortOption(sortOption: SortOption): ItemsListState {
    return copy(sortOption = sortOption)
}

fun ItemsListState.withViewMode(viewMode: ViewMode): ItemsListState {
    return copy(viewMode = viewMode)
}

fun ItemsListState.withSearchQuery(query: String): ItemsListState {
    return copy(searchQuery = query)
}

fun ItemsListState.withSelectedItems(selectedItems: Set<String>): ItemsListState {
    return copy(selectedItems = selectedItems)
}