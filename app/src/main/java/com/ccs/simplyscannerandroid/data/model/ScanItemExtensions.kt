package com.ccs.simplyscannerandroid.data.model

import java.util.UUID
import kotlin.random.Random

/**
 * Extension functions and utilities for ScanItem data model
 */

/**
 * Creates a new document ScanItem with default values
 */
fun createDocument(
    displayName: String,
    pages: List<String> = emptyList()
): ScanItem {
    return ScanItem(
        uuid = UUID.randomUUID().toString(),
        displayName = displayName,
        bDir = false,
        order = pages,
        bLock = false,
        createdDate = System.currentTimeMillis(),
        updatedDate = System.currentTimeMillis(),
        deletedDate = null
    )
}

/**
 * Creates a new folder ScanItem with default values
 */
fun createFolder(
    displayName: String
): ScanItem {
    return ScanItem(
        uuid = UUID.randomUUID().toString(),
        displayName = displayName,
        bDir = true,
        order = emptyList(),
        bLock = false,
        createdDate = System.currentTimeMillis(),
        updatedDate = System.currentTimeMillis(),
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
        pages = listOf("page_1.jpg", "page_2.jpg", "page_3.jpg")
    )
}

/**
 * Creates a sample folder for testing purposes
 */
fun createSampleFolder(): ScanItem {
    return createFolder(
        displayName = "Sample Folder ${Random.nextInt(1000)}"
    )
}