package com.ccs.simplyscannerandroid.data.model

import java.text.SimpleDateFormat
import java.util.UUID
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

/**
 * Extension functions and utilities for ScanItem data model
 */

// Date formatter for directory names following iOS pattern: yyyy-MM-dd_HH_mm_ss.SSS
private val directoryDateFormatter = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss.SSS", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/**
 * Generate directory name from created date
 */
fun generateDirectoryName(createdDate: Long): String {
    return directoryDateFormatter.format(Date(createdDate))
}

/**
 * Generate relativePath according to FR-4.2 requirements
 * @param parentRelativePath The parent folder's relativePath (or "./" for root)
 * @param createdDate The creation date for this item
 */
fun generateRelativePath(parentRelativePath: String = "./", createdDate: Long): String {
    val directoryName = generateDirectoryName(createdDate)
    return "$parentRelativePath$directoryName/"
}

/**
 * Creates a new document ScanItem with default values
 * @param displayName The display name for the document
 * @param pages List of page filenames
 * @param parentRelativePath The parent folder's relativePath (default: root)
 */
fun createDocument(
    displayName: String,
    pages: List<String> = emptyList(),
    parentRelativePath: String = "./"
): ScanItem {
    val createdDate = System.currentTimeMillis()
    return ScanItem(
        uuid = UUID.randomUUID().toString(),
        displayName = displayName,
        bDir = false,
        relativePath = generateRelativePath(parentRelativePath, createdDate),
        order = pages,
        bLock = false,
        createdDate = createdDate,
        updatedDate = createdDate,
        deletedDate = null
    )
}

/**
 * Creates a new folder ScanItem with default values
 * @param displayName The display name for the folder
 * @param parentRelativePath The parent folder's relativePath (default: root)
 */
fun createFolder(
    displayName: String,
    parentRelativePath: String = "./"
): ScanItem {
    val createdDate = System.currentTimeMillis()
    return ScanItem(
        uuid = UUID.randomUUID().toString(),
        displayName = displayName,
        bDir = true,
        relativePath = generateRelativePath(parentRelativePath, createdDate),
        order = emptyList(),
        bLock = false,
        createdDate = createdDate,
        updatedDate = createdDate,
        deletedDate = null
    )
}

/**
 * Extension property to check if the item is a document
 */
val ScanItem.isDocument: Boolean
    get() = !bDir

/**
 * Extension property to check if the item is a folder
 */
val ScanItem.isFolder: Boolean
    get() = bDir

/**
 * Extension property to get the number of pages in a document
 */
val ScanItem.pageCount: Int
    get() = if (isDocument) order.size else 0

/**
 * Extension property to check if the item is deleted
 */
val ScanItem.isDeleted: Boolean
    get() = deletedDate != null

/**
 * Extension function to mark item as deleted
 */
fun ScanItem.markAsDeleted(): ScanItem {
    return copy(
        deletedDate = System.currentTimeMillis(),
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Extension function to restore deleted item
 */
fun ScanItem.restore(): ScanItem {
    return copy(
        deletedDate = null,
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Extension function to update display name
 */
fun ScanItem.updateDisplayName(newName: String): ScanItem {
    return copy(
        displayName = newName,
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Extension function to add a page to document
 */
fun ScanItem.addPage(pageFilename: String): ScanItem {
    require(isDocument) { "Cannot add pages to a folder" }
    return copy(
        order = order + pageFilename,
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Extension function to remove a page from document
 */
fun ScanItem.removePage(pageFilename: String): ScanItem {
    require(isDocument) { "Cannot remove pages from a folder" }
    return copy(
        order = order.filter { it != pageFilename },
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Extension function to reorder pages in document
 */
fun ScanItem.reorderPages(newOrder: List<String>): ScanItem {
    require(isDocument) { "Cannot reorder pages in a folder" }
    require(newOrder.size == order.size) { "New order must contain same number of pages" }
    require(newOrder.toSet() == order.toSet()) { "New order must contain the same pages" }
    
    return copy(
        order = newOrder,
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Extension function to toggle lock status
 */
fun ScanItem.toggleLock(): ScanItem {
    return copy(
        bLock = !bLock,
        updatedDate = System.currentTimeMillis()
    )
}

/**
 * Creates a sample document for testing purposes
 */
fun createSampleDocument(): ScanItem {
    return createDocument(
        displayName = "Sample Document ${Random.nextInt(1000)}",
        pages = listOf("page_1.jpg", "page_2.jpg", "page_3.jpg"),
        parentRelativePath = "./"
    )
}

/**
 * Creates a sample folder for testing purposes
 */
fun createSampleFolder(): ScanItem {
    return createFolder(
        displayName = "Sample Folder ${Random.nextInt(1000)}",
        parentRelativePath = "./"
    )
}