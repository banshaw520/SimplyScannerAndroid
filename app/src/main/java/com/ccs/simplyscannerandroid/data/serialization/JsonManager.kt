package com.ccs.simplyscannerandroid.data.serialization

import com.ccs.simplyscannerandroid.data.model.ScanItem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * JSON serialization manager for ScanItem and related data
 */
object JsonManager {
    
    /**
     * JSON configuration for consistent serialization
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }
    
    /**
     * Serialize ScanItem to JSON string
     */
    fun serializeScanItem(item: ScanItem): String {
        return try {
            json.encodeToString(item)
        } catch (e: SerializationException) {
            throw JsonSerializationException("Failed to serialize ScanItem: ${e.message}", e)
        }
    }
    
    /**
     * Deserialize ScanItem from JSON string
     */
    fun deserializeScanItem(jsonString: String): ScanItem {
        return try {
            json.decodeFromString<ScanItem>(jsonString)
        } catch (e: SerializationException) {
            throw JsonDeserializationException("Failed to deserialize ScanItem: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw JsonDeserializationException("Invalid JSON format: ${e.message}", e)
        }
    }
    
    /**
     * Save ScanItem to JSON file
     */
    @Throws(IOException::class)
    fun saveScanItemToFile(item: ScanItem, file: File) {
        try {
            val jsonString = serializeScanItem(item)
            file.writeText(jsonString)
        } catch (e: JsonSerializationException) {
            throw IOException("Failed to save ScanItem to file: ${e.message}", e)
        } catch (e: Exception) {
            throw IOException("File write error: ${e.message}", e)
        }
    }
    
    /**
     * Load ScanItem from JSON file
     */
    @Throws(IOException::class)
    fun loadScanItemFromFile(file: File): ScanItem {
        if (!file.exists()) {
            throw IOException("File not found: ${file.absolutePath}")
        }
        
        try {
            val jsonString = file.readText()
            return deserializeScanItem(jsonString)
        } catch (e: JsonDeserializationException) {
            throw IOException("Failed to load ScanItem from file: ${e.message}", e)
        } catch (e: Exception) {
            throw IOException("File read error: ${e.message}", e)
        }
    }
    
    /**
     * Validate JSON string format
     */
    fun validateJsonString(jsonString: String): ValidationResult {
        return try {
            json.decodeFromString<ScanItem>(jsonString)
            ValidationResult.Success
        } catch (e: SerializationException) {
            ValidationResult.Error("Invalid JSON format: ${e.message}")
        } catch (e: IllegalArgumentException) {
            ValidationResult.Error("Invalid data structure: ${e.message}")
        }
    }
    
    /**
     * Validate JSON file
     */
    fun validateJsonFile(file: File): ValidationResult {
        if (!file.exists()) {
            return ValidationResult.Error("File not found: ${file.absolutePath}")
        }
        
        return try {
            val jsonString = file.readText()
            validateJsonString(jsonString)
        } catch (e: Exception) {
            ValidationResult.Error("File read error: ${e.message}")
        }
    }
    
    /**
     * Migrate legacy JSON format to current format
     */
    fun migrateLegacyJson(legacyJsonString: String): String {
        return try {
            // For now, just try to parse and re-serialize
            // In a real app, this would handle format migrations
            val item = deserializeScanItem(legacyJsonString)
            serializeScanItem(item)
        } catch (e: Exception) {
            throw JsonMigrationException("Failed to migrate legacy JSON: ${e.message}", e)
        }
    }
    
    /**
     * Create backup of JSON file
     */
    @Throws(IOException::class)
    fun createBackup(sourceFile: File, backupFile: File) {
        try {
            sourceFile.copyTo(backupFile, overwrite = true)
        } catch (e: Exception) {
            throw IOException("Failed to create backup: ${e.message}", e)
        }
    }
    
    /**
     * Restore from backup
     */
    @Throws(IOException::class)
    fun restoreFromBackup(backupFile: File, targetFile: File) {
        try {
            backupFile.copyTo(targetFile, overwrite = true)
        } catch (e: Exception) {
            throw IOException("Failed to restore from backup: ${e.message}", e)
        }
    }
    
    /**
     * Get JSON format version (for future use)
     */
    fun getJsonFormatVersion(): String = "1.0"
    
    /**
     * Pretty print JSON string
     */
    fun prettyPrintJson(jsonString: String): String {
        return try {
            val item = deserializeScanItem(jsonString)
            serializeScanItem(item)
        } catch (e: Exception) {
            jsonString // Return original if parsing fails
        }
    }
    
    /**
     * Minify JSON string
     */
    fun minifyJson(jsonString: String): String {
        return try {
            val compactJson = Json { ignoreUnknownKeys = true }
            val item = json.decodeFromString<ScanItem>(jsonString)
            compactJson.encodeToString(item)
        } catch (e: Exception) {
            jsonString // Return original if parsing fails
        }
    }
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    val isValid: Boolean
        get() = this is Success
    
    val errorMessage: String?
        get() = if (this is Error) message else null
}

/**
 * JSON serialization exceptions
 */
sealed class JsonException(message: String, cause: Throwable? = null) : Exception(message, cause)

class JsonSerializationException(message: String, cause: Throwable? = null) : JsonException(message, cause)
class JsonDeserializationException(message: String, cause: Throwable? = null) : JsonException(message, cause)
class JsonMigrationException(message: String, cause: Throwable? = null) : JsonException(message, cause)
class JsonValidationException(message: String, cause: Throwable? = null) : JsonException(message, cause)