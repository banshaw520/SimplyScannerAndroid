package com.ccs.simplyscannerandroid.data

import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.model.ScanPage
import com.ccs.simplyscannerandroid.data.model.PageProcessingMetadata
import com.ccs.simplyscannerandroid.data.model.generateRelativePath
import java.util.UUID

/**
 * Mock data for testing the document storage system
 * Following the requirements from storage_prd_section.md
 */
object MockData {
    
    // Helper function to generate timestamps for different dates
    private fun daysAgo(days: Int): Long {
        return System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
    }
    
    // Helper function to generate UUID strings
    private fun generateUUID(): String = UUID.randomUUID().toString()
    
    /**
     * Mock ScanItems representing various document types and folders
     * This follows the storage requirements:
     * - Each item has a unique UUID
     * - Items can be folders (bDir = true) or documents (bDir = false)
     * - Documents have ordered page lists
     * - Some items are locked with PIN
     */
    val mockScanItems = listOf(
        // Folder: Work Documents
        ScanItem(
            uuid = generateUUID(),
            displayName = "Work Documents",
            bDir = true,
            relativePath = generateRelativePath("./", daysAgo(30)),
            order = emptyList(), // Folders don't have page order
            bLock = false,
            createdDate = daysAgo(30),
            updatedDate = daysAgo(5)
        ),
        
        // Document: Contract Agreement (Multi-page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Contract Agreement",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(15)),
            order = listOf("page_1.jpg", "page_2.jpg", "page_3.jpg", "page_4.jpg"),
            bLock = true, // Locked with PIN
            createdDate = daysAgo(15),
            updatedDate = daysAgo(10)
        ),
        
        // Document: Receipt - Coffee Shop (Single page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Receipt - Coffee Shop",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(7)),
            order = listOf("page_1.jpg"),
            bLock = false,
            createdDate = daysAgo(7),
            updatedDate = daysAgo(7)
        ),
        
        // Folder: Personal Documents (Locked)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Personal Documents",
            bDir = true,
            relativePath = generateRelativePath("./", daysAgo(45)),
            order = emptyList(),
            bLock = true, // Locked folder
            createdDate = daysAgo(45),
            updatedDate = daysAgo(2)
        ),
        
        // Document: Insurance Policy (Multi-page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Insurance Policy",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(20)),
            order = listOf("page_1.jpg", "page_2.jpg", "page_3.jpg", "page_4.jpg", "page_5.jpg", "page_6.jpg"),
            bLock = false,
            createdDate = daysAgo(20),
            updatedDate = daysAgo(18)
        ),
        
        // Document: Business Card (Single page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Business Card - John Doe",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(3)),
            order = listOf("page_1.jpg"),
            bLock = false,
            createdDate = daysAgo(3),
            updatedDate = daysAgo(3)
        ),
        
        // Folder: Receipts
        ScanItem(
            uuid = generateUUID(),
            displayName = "Receipts",
            bDir = true,
            relativePath = generateRelativePath("./", daysAgo(60)),
            order = emptyList(),
            bLock = false,
            createdDate = daysAgo(60),
            updatedDate = daysAgo(1)
        ),
        
        // Document: Passport (Multi-page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Passport",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(90)),
            order = listOf("page_1.jpg", "page_2.jpg"),
            bLock = true, // Sensitive document - locked
            createdDate = daysAgo(90),
            updatedDate = daysAgo(85)
        ),
        
        // Document: Meeting Notes (Multi-page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Meeting Notes - Q4 Planning",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(1)),
            order = listOf("page_1.jpg", "page_2.jpg", "page_3.jpg"),
            bLock = false,
            createdDate = daysAgo(1),
            updatedDate = daysAgo(1)
        ),
        
        // Document: Tax Document (Multi-page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Tax Document 2024",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(120)),
            order = listOf("page_1.jpg", "page_2.jpg", "page_3.jpg", "page_4.jpg", "page_5.jpg"),
            bLock = true, // Sensitive document - locked
            createdDate = daysAgo(120),
            updatedDate = daysAgo(100)
        ),
        
        // Folder: Archive
        ScanItem(
            uuid = generateUUID(),
            displayName = "Archive",
            bDir = true,
            relativePath = generateRelativePath("./", daysAgo(365)),
            order = emptyList(),
            bLock = false,
            createdDate = daysAgo(365),
            updatedDate = daysAgo(30)
        ),
        
        // Document: Invoice (Single page)
        ScanItem(
            uuid = generateUUID(),
            displayName = "Invoice #12345",
            bDir = false,
            relativePath = generateRelativePath("./", daysAgo(12)),
            order = listOf("page_1.jpg"),
            bLock = false,
            createdDate = daysAgo(12),
            updatedDate = daysAgo(12)
        )
    )
    
    /**
     * Mock ScanPages for testing page-level operations
     */
    val mockScanPages = listOf(
        ScanPage(
            filename = "page_1.jpg",
            displayName = "Page 1",
            createdDate = daysAgo(1),
            processingMetadata = PageProcessingMetadata(
                originalSize = Pair(1920, 1080),
                croppedSize = Pair(1800, 1000),
                rotation = 0f,
                brightness = 0.1f,
                contrast = 0.2f,
                sharpness = 0.1f
            )
        ),
        ScanPage(
            filename = "page_2.jpg",
            displayName = "Page 2",
            createdDate = daysAgo(1),
            processingMetadata = PageProcessingMetadata(
                originalSize = Pair(1920, 1080),
                croppedSize = Pair(1850, 1050),
                rotation = 0f,
                brightness = 0f,
                contrast = 0.1f,
                sharpness = 0f
            )
        ),
        ScanPage(
            filename = "page_3.jpg",
            displayName = "Page 3",
            createdDate = daysAgo(1),
            processingMetadata = PageProcessingMetadata(
                originalSize = Pair(1920, 1080),
                croppedSize = Pair(1800, 1000),
                rotation = 0f,
                brightness = -0.1f,
                contrast = 0.3f,
                sharpness = 0.2f
            )
        )
    )
    
    /**
     * Helper function to get documents only (not folders)
     */
    fun getDocuments(): List<ScanItem> {
        return mockScanItems.filter { !it.bDir }
    }
    
    /**
     * Helper function to get folders only
     */
    fun getFolders(): List<ScanItem> {
        return mockScanItems.filter { it.bDir }
    }
    
    /**
     * Helper function to get locked items
     */
    fun getLockedItems(): List<ScanItem> {
        return mockScanItems.filter { it.bLock }
    }
    
    /**
     * Helper function to get recently created items (last 7 days)
     */
    fun getRecentItems(): List<ScanItem> {
        val sevenDaysAgo = daysAgo(7)
        return mockScanItems.filter { it.createdDate > sevenDaysAgo }
    }
    
    /**
     * Helper function to simulate a desc.json file content for a document
     */
    fun generateDescJson(scanItem: ScanItem): String {
        return """
        {
            "uuid": "${scanItem.uuid}",
            "displayName": "${scanItem.displayName}",
            "bDir": ${scanItem.bDir},
            "order": [${scanItem.order.joinToString(", ") { "\"$it\"" }}],
            "bLock": ${scanItem.bLock},
            "createdDate": ${scanItem.createdDate},
            "updatedDate": ${scanItem.updatedDate},
            "deletedDate": ${scanItem.deletedDate}
        }
        """.trimIndent()
    }
    
    /**
     * Helper function to simulate directory structure for testing
     */
    fun getDirectoryStructure(): Map<String, List<String>> {
        val structure = mutableMapOf<String, List<String>>()
        
        mockScanItems.forEach { item ->
            val folderName = item.uuid
            val files = mutableListOf<String>()
            
            // Always include desc.json
            files.add("desc.json")
            
            // Add page images if it's a document
            if (!item.bDir) {
                files.addAll(item.order)
            }
            
            structure[folderName] = files
        }
        
        return structure
    }
}