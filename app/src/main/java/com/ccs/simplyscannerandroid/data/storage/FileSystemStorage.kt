package com.ccs.simplyscannerandroid.data.storage

import android.content.Context
import android.os.Environment
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.serialization.JsonManager
import com.ccs.simplyscannerandroid.data.serialization.ValidationResult
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * File system storage manager for ScanItem documents
 * Implements iOS-compatible directory structure with desc.json metadata files
 * 
 * Storage Strategy:
 * - Development/Debug: Public Documents directory (accessible for testing)
 * - Production/Release: Secure internal app storage
 */
class FileSystemStorage(private val context: Context) {
    
    companion object {
        private const val STORAGE_DIR_NAME = "SimplyScannerPro"
        private const val DESC_FILE_NAME = "desc.json"
        private const val TEMP_DIR_NAME = "temp"
        private const val BACKUP_DIR_NAME = "backup"
    }
    
    // Date formatter for directory names following iOS pattern: yyyy-MM-dd_HH_mm_ss.SSS
    private val directoryDateFormatter = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Get the root storage directory based on build configuration
     */
    private fun getStorageRoot(): File {
        return if (isDebugBuild()) {
            // Development: Use public Documents directory for easy access
            getPublicDocumentsDirectory()
        } else {
            // Production: Use secure internal storage
            getSecureInternalDirectory()
        }
    }
    
    /**
     * Check if running in debug mode
     */
    private fun isDebugBuild(): Boolean {
        return try {
            // Use ApplicationInfo flags to detect debug build
            val appInfo = context.applicationInfo
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false // Default to production mode if detection fails
        }
    }
    
    /**
     * Get public Documents directory for development
     * Path: /storage/emulated/0/Documents/SimplyScannerPro/
     */
    private fun getPublicDocumentsDirectory(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(documentsDir, STORAGE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get secure internal storage for production
     * Path: /data/data/com.ccs.simplyscannerandroid/files/SimplyScannerPro/
     */
    private fun getSecureInternalDirectory(): File {
        return File(context.filesDir, STORAGE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get current storage location info for debugging
     */
    fun getStorageInfo(): StorageInfo {
        val root = getStorageRoot()
        return StorageInfo(
            isPublicStorage = isDebugBuild(),
            rootPath = root.absolutePath,
            isAccessible = root.exists() && root.canWrite(),
            totalSpace = root.totalSpace,
            freeSpace = root.freeSpace
        )
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
     * Get the directory for a specific document/folder using formatted date name
     */
    fun getItemDirectory(item: ScanItem): File {
        val directoryName = directoryDateFormatter.format(Date(item.createdDate))
        return File(getStorageRoot(), directoryName)
    }
    
    /**
     * Get the desc.json file for a specific item
     */
    fun getDescFile(item: ScanItem): File {
        return File(getItemDirectory(item), DESC_FILE_NAME)
    }
    
    /**
     * Get the page file for a specific document
     */
    fun getPageFile(item: ScanItem, pageFilename: String): File {
        return File(getItemDirectory(item), pageFilename)
    }
    
    /**
     * Get the page file for a specific document using UUID (for FileOperations compatibility)
     */
    fun getPageFile(uuid: String, pageFilename: String): File {
        // For FileOperations compatibility, we need to find the item by UUID first
        return try {
            val item = loadItemMetadata(uuid)
            getPageFile(item, pageFilename)
        } catch (e: Exception) {
            // Fallback: this shouldn't happen in the optimized version, but kept for safety
            throw IOException("Could not find item with UUID: $uuid")
        }
    }
    
    /**
     * Create directory structure for a new item using date-based naming
     */
    fun createItemDirectory(item: ScanItem): File {
        val itemDir = getItemDirectory(item)
        if (!itemDir.exists()) {
            itemDir.mkdirs()
        }
        return itemDir
    }
    
    /**
     * Save ScanItem metadata to desc.json using date-based directory naming
     */
    @Throws(IOException::class)
    fun saveItemMetadata(item: ScanItem) {
        val itemDir = createItemDirectory(item)
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
        // Search all date-based directories to find the item with matching UUID
        val storageRoot = getStorageRoot()
        val foundDir = storageRoot.listFiles()?.find { dir ->
            if (dir.isDirectory && dir.name != TEMP_DIR_NAME && dir.name != BACKUP_DIR_NAME) {
                val descFile = File(dir, DESC_FILE_NAME)
                if (descFile.exists()) {
                    try {
                        val item = JsonManager.loadScanItemFromFile(descFile)
                        item.uuid == uuid
                    } catch (e: Exception) {
                        false
                    }
                } else false
            } else false
        }
        
        if (foundDir == null) {
            throw IOException("Metadata file not found for item: $uuid")
        }
        
        val descFile = File(foundDir, DESC_FILE_NAME)
        try {
            return JsonManager.loadScanItemFromFile(descFile)
        } catch (e: Exception) {
            throw IOException("Failed to load item metadata: ${e.message}", e)
        }
    }
    
    /**
     * Validate item metadata file
     */
    fun validateItemMetadata(item: ScanItem): ValidationResult {
        val descFile = getDescFile(item)
        return JsonManager.validateJsonFile(descFile)
    }
    
    /**
     * Delete item directory and all its contents
     */
    fun deleteItemDirectory(item: ScanItem): Boolean {
        val itemDir = getItemDirectory(item)
        return itemDir.deleteRecursively()
    }
    
    /**
     * Delete item directory by UUID (searches date-based directories)
     */
    fun deleteItemDirectory(uuid: String): Boolean {
        return try {
            val item = loadItemMetadata(uuid)
            deleteItemDirectory(item)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * List all item UUIDs from date-based directories
     */
    fun listItemDirectories(): List<String> {
        val storageRoot = getStorageRoot()
        return storageRoot.listFiles()?.mapNotNull { dir ->
            if (dir.isDirectory && 
                dir.name != TEMP_DIR_NAME && 
                dir.name != BACKUP_DIR_NAME &&
                File(dir, DESC_FILE_NAME).exists()) {
                try {
                    val descFile = File(dir, DESC_FILE_NAME)
                    val item = JsonManager.loadScanItemFromFile(descFile)
                    item.uuid
                } catch (e: Exception) {
                    null
                }
            } else null
        } ?: emptyList()
    }
    
    /**
     * Check if item directory exists
     */
    fun itemDirectoryExists(item: ScanItem): Boolean {
        return getItemDirectory(item).exists()
    }
    
    /**
     * Check if desc.json exists for item
     */
    fun itemMetadataExists(item: ScanItem): Boolean {
        return getDescFile(item).exists()
    }
    
    /**
     * Get the size of an item directory in bytes
     */
    fun getItemDirectorySize(item: ScanItem): Long {
        val itemDir = getItemDirectory(item)
        return if (itemDir.exists()) {
            itemDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
    }
    
    /**
     * Get the size of an item directory in bytes by UUID
     */
    fun getItemDirectorySize(uuid: String): Long {
        return try {
            val item = loadItemMetadata(uuid)
            getItemDirectorySize(item)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Create a backup of item directory
     */
    @Throws(IOException::class)
    fun backupItemDirectory(item: ScanItem): File {
        val itemDir = getItemDirectory(item)
        val backupDir = getBackupDirectory()
        val backupFile = File(backupDir, "${item.uuid}_${System.currentTimeMillis()}.backup")
        
        if (!itemDir.exists()) {
            throw IOException("Item directory not found: ${item.uuid}")
        }
        
        try {
            itemDir.copyRecursively(backupFile)
            return backupFile
        } catch (e: Exception) {
            throw IOException("Failed to create backup: ${e.message}", e)
        }
    }
    
    /**
     * Create a backup of item directory by UUID
     */
    @Throws(IOException::class)
    fun backupItemDirectory(uuid: String): File {
        val item = loadItemMetadata(uuid)
        return backupItemDirectory(item)
    }
    
    /**
     * Restore item directory from backup
     */
    @Throws(IOException::class)
    fun restoreItemDirectory(item: ScanItem, backupFile: File) {
        if (!backupFile.exists()) {
            throw IOException("Backup file not found: ${backupFile.absolutePath}")
        }
        
        val itemDir = getItemDirectory(item)
        
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
 * Storage information data class
 */
data class StorageInfo(
    val isPublicStorage: Boolean,
    val rootPath: String,
    val isAccessible: Boolean,
    val totalSpace: Long,
    val freeSpace: Long
)

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