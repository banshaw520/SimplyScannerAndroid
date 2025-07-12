package com.ccs.simplyscannerandroid.data.model

import kotlinx.serialization.Serializable

/**
 * Core data model representing a scanned document or folder
 * This class mirrors the iOS ScanItem structure for cross-platform compatibility
 */
@Serializable
data class ScanItem(
    val uuid: String,
    val displayName: String,
    val bDir: Boolean, // true for folders, false for documents
    val relativePath: String, // cumulative path representing nested location in file system
    val order: List<String> = emptyList(), // ordered list of page filenames
    val bLock: Boolean = false, // PIN lock status
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val deletedDate: Long? = null // null if not deleted
)

/**
 * Represents a single scanned page within a document
 */
@Serializable
data class ScanPage(
    val filename: String,
    val displayName: String = filename,
    val createdDate: Long = System.currentTimeMillis(),
    val processingMetadata: PageProcessingMetadata? = null
)

/**
 * Metadata for page processing operations
 */
@Serializable
data class PageProcessingMetadata(
    val originalSize: Pair<Int, Int>,
    val croppedSize: Pair<Int, Int>,
    val rotation: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val sharpness: Float = 0f
)

/**
 * Enum for document sorting options
 */
enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC
}

/**
 * Enum for document view modes
 */
enum class ViewMode {
    LIST,
    GRID
}