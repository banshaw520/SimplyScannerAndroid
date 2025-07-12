package com.ccs.simplyscannerandroid.data.serialization

import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.model.generateRelativePath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Serialization utilities and extensions
 */
object SerializationUtils {
    
    /**
     * Create a snapshot of current data for backup
     */
    @Serializable
    data class DataSnapshot(
        val version: String,
        val createdAt: Long,
        val items: List<ScanItem>,
        val metadata: SnapshotMetadata
    )
    
    @Serializable
    data class SnapshotMetadata(
        val appVersion: String,
        val deviceModel: String,
        val androidVersion: Int,
        val totalItems: Int,
        val totalDocuments: Int,
        val totalFolders: Int
    )
    
    /**
     * Export data to JSON snapshot
     */
    fun exportToSnapshot(items: List<ScanItem>): DataSnapshot {
        val metadata = SnapshotMetadata(
            appVersion = "1.0.0", // This should come from BuildConfig
            deviceModel = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.SDK_INT,
            totalItems = items.size,
            totalDocuments = items.count { !it.bDir },
            totalFolders = items.count { it.bDir }
        )
        
        return DataSnapshot(
            version = JsonManager.getJsonFormatVersion(),
            createdAt = System.currentTimeMillis(),
            items = items,
            metadata = metadata
        )
    }
    
    /**
     * Import data from JSON snapshot
     */
    fun importFromSnapshot(snapshotJson: String): DataSnapshot {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString(snapshotJson)
    }
    
    /**
     * Create filename for snapshot
     */
    fun createSnapshotFilename(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = dateFormat.format(Date())
        return "scanner_backup_$timestamp.json"
    }
    
    /**
     * Validate data integrity
     */
    fun validateDataIntegrity(items: List<ScanItem>): IntegrityResult {
        val issues = mutableListOf<String>()
        
        // Check for duplicate UUIDs
        val uuids = items.map { it.uuid }
        val duplicateUuids = uuids.groupingBy { it }.eachCount().filterValues { it > 1 }
        if (duplicateUuids.isNotEmpty()) {
            issues.add("Duplicate UUIDs found: ${duplicateUuids.keys}")
        }
        
        // Check for invalid dates
        items.forEach { item ->
            if (item.createdDate > System.currentTimeMillis()) {
                issues.add("Future creation date for item: ${item.uuid}")
            }
            if (item.updatedDate < item.createdDate) {
                issues.add("Update date before creation date for item: ${item.uuid}")
            }
            item.deletedDate?.let { deletedDate ->
                if (deletedDate < item.createdDate) {
                    issues.add("Delete date before creation date for item: ${item.uuid}")
                }
            }
        }
        
        // Check for empty display names
        items.forEach { item ->
            if (item.displayName.isBlank()) {
                issues.add("Empty display name for item: ${item.uuid}")
            }
        }
        
        // Check for invalid page order in documents
        items.filter { !it.bDir }.forEach { document ->
            if (document.order.isEmpty()) {
                issues.add("Document has no pages: ${document.uuid}")
            }
            val duplicatePages = document.order.groupingBy { it }.eachCount().filterValues { it > 1 }
            if (duplicatePages.isNotEmpty()) {
                issues.add("Duplicate pages in document ${document.uuid}: ${duplicatePages.keys}")
            }
        }
        
        return IntegrityResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Repair data integrity issues
     */
    fun repairDataIntegrity(items: List<ScanItem>): List<ScanItem> {
        return items.map { item ->
            var repairedItem = item
            
            // Fix empty display names
            if (item.displayName.isBlank()) {
                repairedItem = repairedItem.copy(
                    displayName = if (item.bDir) "Untitled Folder" else "Untitled Document"
                )
            }
            
            // Fix invalid dates
            if (item.updatedDate < item.createdDate) {
                repairedItem = repairedItem.copy(updatedDate = item.createdDate)
            }
            
            // Fix duplicate pages in documents
            if (!item.bDir && item.order.isNotEmpty()) {
                val uniquePages = item.order.distinct()
                if (uniquePages.size != item.order.size) {
                    repairedItem = repairedItem.copy(order = uniquePages)
                }
            }
            
            repairedItem
        }.distinctBy { it.uuid } // Remove duplicate UUIDs
    }
    
    /**
     * Convert ScanItem to legacy format (for compatibility)
     */
    fun toLegacyFormat(item: ScanItem): Map<String, Any?> {
        return mapOf(
            "uuid" to item.uuid,
            "displayName" to item.displayName,
            "isDirectory" to item.bDir,
            "relativePath" to item.relativePath,
            "pages" to item.order,
            "isLocked" to item.bLock,
            "createdDate" to item.createdDate,
            "updatedDate" to item.updatedDate,
            "deletedDate" to item.deletedDate
        )
    }
    
    /**
     * Convert from legacy format to ScanItem
     */
    fun fromLegacyFormat(legacyData: Map<String, Any?>): ScanItem {
        val createdDate = legacyData["createdDate"] as? Long ?: System.currentTimeMillis()
        return ScanItem(
            uuid = legacyData["uuid"] as? String ?: UUID.randomUUID().toString(),
            displayName = legacyData["displayName"] as? String ?: "Untitled",
            bDir = legacyData["isDirectory"] as? Boolean ?: false,
            relativePath = legacyData["relativePath"] as? String ?: generateRelativePath("./", createdDate),
            order = (legacyData["pages"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            bLock = legacyData["isLocked"] as? Boolean ?: false,
            createdDate = createdDate,
            updatedDate = legacyData["updatedDate"] as? Long ?: System.currentTimeMillis(),
            deletedDate = legacyData["deletedDate"] as? Long
        )
    }
    
    /**
     * Calculate JSON file size
     */
    fun calculateJsonSize(item: ScanItem): Long {
        return try {
            JsonManager.serializeScanItem(item).toByteArray().size.toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get JSON statistics
     */
    fun getJsonStatistics(items: List<ScanItem>): JsonStatistics {
        val totalSize = items.sumOf { calculateJsonSize(it) }
        val averageSize = if (items.isNotEmpty()) totalSize / items.size else 0L
        
        return JsonStatistics(
            totalItems = items.size,
            totalSizeBytes = totalSize,
            averageSizeBytes = averageSize,
            largestItemUuid = items.maxByOrNull { calculateJsonSize(it) }?.uuid,
            smallestItemUuid = items.minByOrNull { calculateJsonSize(it) }?.uuid
        )
    }
}

/**
 * Data integrity result
 */
data class IntegrityResult(
    val isValid: Boolean,
    val issues: List<String>
)

/**
 * JSON statistics
 */
data class JsonStatistics(
    val totalItems: Int,
    val totalSizeBytes: Long,
    val averageSizeBytes: Long,
    val largestItemUuid: String?,
    val smallestItemUuid: String?
)

/**
 * Extension functions for easier serialization
 */
fun ScanItem.toJsonString(): String = JsonManager.serializeScanItem(this)

fun ScanItem.saveToFile(file: File) = JsonManager.saveScanItemToFile(this, file)

fun File.loadAsScanItem(): ScanItem = JsonManager.loadScanItemFromFile(this)

fun String.toScanItem(): ScanItem = JsonManager.deserializeScanItem(this)

fun List<ScanItem>.validateIntegrity(): IntegrityResult = SerializationUtils.validateDataIntegrity(this)

fun List<ScanItem>.repairIntegrity(): List<ScanItem> = SerializationUtils.repairDataIntegrity(this)

fun List<ScanItem>.exportToSnapshot(): SerializationUtils.DataSnapshot = SerializationUtils.exportToSnapshot(this)