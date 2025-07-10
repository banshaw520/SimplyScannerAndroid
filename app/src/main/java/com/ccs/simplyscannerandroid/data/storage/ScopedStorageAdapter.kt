package com.ccs.simplyscannerandroid.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.ccs.simplyscannerandroid.data.permissions.PermissionManager
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Scoped storage adapter for Android Q+ compatibility
 */
class ScopedStorageAdapter(private val context: Context) {
    
    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.ccs.simplyscannerandroid.fileprovider"
        private const val MIME_TYPE_IMAGE = "image/*"
        private const val MIME_TYPE_PDF = "application/pdf"
    }
    
    /**
     * Check if scoped storage is available
     */
    fun isScopedStorageAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    /**
     * Check if scoped storage is enforced
     */
    fun isScopedStorageEnforced(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
    
    /**
     * Get app-specific external storage directory
     */
    fun getAppExternalStorageDir(): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }
    
    /**
     * Get app-specific cache directory
     */
    fun getAppCacheDir(): File {
        return context.externalCacheDir ?: context.cacheDir
    }
    
    /**
     * Check if external storage is available
     */
    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Check if external storage is writable
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Create content URI for sharing files
     */
    fun createShareableUri(file: File): Uri? {
        return try {
            if (file.exists()) {
                FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create sharing intent for document
     */
    fun createSharingIntent(file: File, mimeType: String = MIME_TYPE_PDF): Intent? {
        val uri = createShareableUri(file) ?: return null
        
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Create multiple sharing intent for multiple files
     */
    fun createMultipleSharingIntent(files: List<File>, mimeType: String = MIME_TYPE_IMAGE): Intent? {
        val uris = files.mapNotNull { createShareableUri(it) }
        if (uris.isEmpty()) return null
        
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Copy file from URI to app storage
     */
    @Throws(IOException::class)
    fun copyFromUri(sourceUri: Uri, targetFile: File) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open input stream from URI")
    }
    
    /**
     * Copy file to URI
     */
    @Throws(IOException::class)
    fun copyToUri(sourceFile: File, targetUri: Uri) {
        if (!sourceFile.exists()) {
            throw IOException("Source file does not exist: ${sourceFile.absolutePath}")
        }
        
        context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open output stream to URI")
    }
    
    /**
     * Get file details from URI
     */
    fun getFileDetails(uri: Uri): FileDetails? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    
                    FileDetails(
                        name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown",
                        size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                        mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else "unknown"
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create document picker intent
     */
    fun createDocumentPickerIntent(mimeType: String = MIME_TYPE_IMAGE): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }
    
    /**
     * Create document save intent
     */
    fun createDocumentSaveIntent(fileName: String, mimeType: String = MIME_TYPE_PDF): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
    
    /**
     * Create camera image capture intent
     */
    fun createCameraIntent(): Pair<Intent, Uri>? {
        return try {
            val tempFile = File(getAppCacheDir(), "temp_camera_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, tempFile)
            
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            
            Pair(intent, photoUri)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles() {
        val cacheDir = getAppCacheDir()
        val tempFiles = cacheDir.listFiles { file ->
            file.name.startsWith("temp_") && 
            (System.currentTimeMillis() - file.lastModified()) > 24 * 60 * 60 * 1000 // 24 hours
        }
        
        tempFiles?.forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
    
    /**
     * Get storage usage information
     */
    fun getStorageUsage(): StorageUsage {
        val externalDir = getAppExternalStorageDir()
        val cacheDir = getAppCacheDir()
        
        return StorageUsage(
            totalSpace = externalDir.totalSpace,
            freeSpace = externalDir.freeSpace,
            usedSpace = externalDir.totalSpace - externalDir.freeSpace,
            appDataSize = calculateDirectorySize(externalDir),
            cacheSize = calculateDirectorySize(cacheDir),
            isExternalAvailable = isExternalStorageAvailable(),
            isExternalWritable = isExternalStorageWritable()
        )
    }
    
    /**
     * Calculate directory size recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        return try {
            directory.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if app has required permissions for storage operations
     */
    fun hasRequiredPermissions(): Boolean {
        return PermissionManager.hasAllRequiredPermissions(context)
    }
    
    /**
     * Get missing permissions
     */
    fun getMissingPermissions(): List<String> {
        return PermissionManager.getMissingPermissions(context)
    }
}

/**
 * File details data class
 */
data class FileDetails(
    val name: String,
    val size: Long,
    val mimeType: String
)

/**
 * Storage usage data class
 */
data class StorageUsage(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long,
    val appDataSize: Long,
    val cacheSize: Long,
    val isExternalAvailable: Boolean,
    val isExternalWritable: Boolean
)