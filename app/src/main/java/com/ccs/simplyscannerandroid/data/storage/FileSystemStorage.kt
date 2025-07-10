package com.ccs.simplyscannerandroid.data.storage

import android.content.Context
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.serialization.JsonManager
import com.ccs.simplyscannerandroid.data.serialization.ValidationResult
import java.io.File
import java.io.IOException

/**
 * File system storage manager for ScanItem documents
 * Implements iOS-compatible directory structure with desc.json metadata files
 */
class FileSystemStorage(private val context: Context) {
    
    companion object {
        private const val STORAGE_DIR_NAME = "SimplyScannerPro"
        private const val DESC_FILE_NAME = "desc.json"
        private const val TEMP_DIR_NAME = "temp"
        private const val BACKUP_DIR_NAME = "backup"
    }
    
    /**
     * Get the root storage directory
     */
    private fun getStorageRoot(): File {
        return File(context.getExternalFilesDir(null), STORAGE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get the temporary directory for processing
     */
    fun getTempDirectory(): File {
        return File(getStorageRoot(), TEMP_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get the backup directory
     */
    fun getBackupDirectory(): File {
        return File(getStorageRoot(), BACKUP_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get the directory for a specific document/folder
     */
    fun getItemDirectory(uuid: String): File {
        return File(getStorageRoot(), uuid)
    }
    
    /**
     * Get the desc.json file for a specific item
     */
    fun getDescFile(uuid: String): File {
        return File(getItemDirectory(uuid), DESC_FILE_NAME)
    }
    
    /**
     * Get the page file for a specific document
     */
    fun getPageFile(uuid: String, pageFilename: String): File {
        return File(getItemDirectory(uuid), pageFilename)
    }
    
    /**
     * Create directory structure for a new item
     */
    fun createItemDirectory(uuid: String): File {
        val itemDir = getItemDirectory(uuid)
        if (!itemDir.exists()) {
            itemDir.mkdirs()
        }
        return itemDir
    }
    
    /**
     * Save ScanItem metadata to desc.json
     */
    @Throws(IOException::class)
    fun saveItemMetadata(item: ScanItem) {
        val itemDir = createItemDirectory(item.uuid)
        val descFile = File(itemDir, DESC_FILE_NAME)
        
        try {
            JsonManager.saveScanItemToFile(item, descFile)
        } catch (e: Exception) {
            throw IOException("Failed to save item metadata: ${e.message}", e)
        }
    }
    
    /**
     * Load ScanItem metadata from desc.json
     */
    @Throws(IOException::class)
    fun loadItemMetadata(uuid: String): ScanItem {
        val descFile = getDescFile(uuid)
        
        if (!descFile.exists()) {
            throw IOException("Metadata file not found for item: $uuid")
        }
        
        try {
            return JsonManager.loadScanItemFromFile(descFile)
        } catch (e: Exception) {
            throw IOException("Failed to load item metadata: ${e.message}", e)
        }
    }
    
    /**
     * Validate item metadata file
     */
    fun validateItemMetadata(uuid: String): ValidationResult {
        val descFile = getDescFile(uuid)
        return JsonManager.validateJsonFile(descFile)
    }
    
    /**
     * Delete item directory and all its contents
     */
    fun deleteItemDirectory(uuid: String): Boolean {
        val itemDir = getItemDirectory(uuid)
        return itemDir.deleteRecursively()
    }
    
    /**
     * List all item directories (UUIDs)
     */
    fun listItemDirectories(): List<String> {
        val storageRoot = getStorageRoot()
        return storageRoot.listFiles()?.filter { 
            it.isDirectory && 
            it.name != TEMP_DIR_NAME && 
            it.name != BACKUP_DIR_NAME &&
            File(it, DESC_FILE_NAME).exists()
        }?.map { it.name } ?: emptyList()
    }
    
    /**
     * Check if item directory exists
     */
    fun itemDirectoryExists(uuid: String): Boolean {
        return getItemDirectory(uuid).exists()
    }
    
    /**
     * Check if desc.json exists for item
     */
    fun itemMetadataExists(uuid: String): Boolean {
        return getDescFile(uuid).exists()
    }
    
    /**
     * Get the size of an item directory in bytes
     */
    fun getItemDirectorySize(uuid: String): Long {
        val itemDir = getItemDirectory(uuid)
        return if (itemDir.exists()) {
            itemDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
    }
    
    /**
     * Create a backup of item directory
     */
    @Throws(IOException::class)
    fun backupItemDirectory(uuid: String): File {
        val itemDir = getItemDirectory(uuid)
        val backupDir = getBackupDirectory()
        val backupFile = File(backupDir, "${uuid}_${System.currentTimeMillis()}.backup")
        
        if (!itemDir.exists()) {
            throw IOException("Item directory not found: $uuid")
        }
        
        try {
            itemDir.copyRecursively(backupFile)
            return backupFile
        } catch (e: Exception) {
            throw IOException("Failed to create backup: ${e.message}", e)
        }
    }
    
    /**
     * Restore item directory from backup
     */
    @Throws(IOException::class)
    fun restoreItemDirectory(uuid: String, backupFile: File) {
        if (!backupFile.exists()) {
            throw IOException("Backup file not found: ${backupFile.absolutePath}")
        }
        
        val itemDir = getItemDirectory(uuid)
        
        try {
            // Delete existing directory if it exists
            if (itemDir.exists()) {
                itemDir.deleteRecursively()
            }
            
            // Restore from backup
            backupFile.copyRecursively(itemDir)
        } catch (e: Exception) {
            throw IOException("Failed to restore from backup: ${e.message}", e)
        }
    }
    
    /**
     * Clean up temporary files older than specified time
     */
    fun cleanupTempFiles(olderThanMillis: Long = 24 * 60 * 60 * 1000) { // 24 hours default
        val tempDir = getTempDirectory()
        val cutoffTime = System.currentTimeMillis() - olderThanMillis
        
        tempDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.deleteRecursively()
            }
        }
    }
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(): StorageStats {
        val storageRoot = getStorageRoot()
        val itemDirectories = listItemDirectories()
        
        val totalSize = itemDirectories.sumOf { getItemDirectorySize(it) }
        val itemCount = itemDirectories.size
        val availableSpace = storageRoot.freeSpace
        val usedSpace = storageRoot.totalSpace - availableSpace
        
        return StorageStats(
            totalItems = itemCount,
            totalSizeBytes = totalSize,
            availableSpaceBytes = availableSpace,
            usedSpaceBytes = usedSpace,
            storageRootPath = storageRoot.absolutePath
        )
    }
}

/**
 * Storage statistics data class
 */
data class StorageStats(
    val totalItems: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long,
    val usedSpaceBytes: Long,
    val storageRootPath: String
)