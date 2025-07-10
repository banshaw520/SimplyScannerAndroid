package com.ccs.simplyscannerandroid.data.service

/**
 * Custom exceptions for ScanService operations
 */
sealed class ScanServiceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Item not found exception
     */
    class ItemNotFoundException(uuid: String) : ScanServiceException("Item not found: $uuid")
    
    /**
     * Invalid operation exception
     */
    class InvalidOperationException(message: String) : ScanServiceException(message)
    
    /**
     * Storage exception
     */
    class StorageException(message: String, cause: Throwable? = null) : ScanServiceException(message, cause)
    
    /**
     * File operation exception
     */
    class FileOperationException(message: String, cause: Throwable? = null) : ScanServiceException(message, cause)
    
    /**
     * Metadata corruption exception
     */
    class MetadataCorruptedException(uuid: String, cause: Throwable? = null) : 
        ScanServiceException("Metadata corrupted for item: $uuid", cause)
    
    /**
     * Permission denied exception
     */
    class PermissionDeniedException(message: String) : ScanServiceException(message)
    
    /**
     * Insufficient storage exception
     */
    class InsufficientStorageException(message: String) : ScanServiceException(message)
}

/**
 * Extension functions for Result handling
 */
fun <T> Result<T>.onStorageFailure(action: (ScanServiceException) -> Unit): Result<T> {
    return onFailure { throwable ->
        val exception = when (throwable) {
            is ScanServiceException -> throwable
            else -> ScanServiceException.StorageException(
                throwable.message ?: "Unknown storage error",
                throwable
            )
        }
        action(exception)
    }
}

/**
 * Extension to convert IOException to ScanServiceException
 */
fun <T> Result<T>.mapStorageException(): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable ->
            val exception = when (throwable) {
                is ScanServiceException -> throwable
                is java.io.IOException -> ScanServiceException.StorageException(
                    throwable.message ?: "Storage operation failed",
                    throwable
                )
                is SecurityException -> ScanServiceException.PermissionDeniedException(
                    throwable.message ?: "Permission denied"
                )
                else -> ScanServiceException.StorageException(
                    throwable.message ?: "Unknown error",
                    throwable
                )
            }
            Result.failure(exception)
        }
    )
}