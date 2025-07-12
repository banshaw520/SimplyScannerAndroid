package com.ccs.simplyscannerandroid.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Utility service for processing imported images
 * Handles resizing, rotation, compression, and optimization
 */
class ImageProcessingService(private val context: Context) {
    
    companion object {
        private const val MAX_IMAGE_WIDTH = 1920
        private const val MAX_IMAGE_HEIGHT = 1080
        private const val JPEG_QUALITY = 85
    }
    
    /**
     * Processes an imported image for document scanning
     * @param inputPath Path to the original image file
     * @param outputDir Directory to save processed image
     * @return ProcessedImageResult containing the processed image info
     */
    suspend fun processImportedImage(
        inputPath: String,
        outputDir: File
    ): Result<ProcessedImageResult> {
        return withContext(Dispatchers.IO) {
            try {
                val inputFile = File(inputPath)
                if (!inputFile.exists()) {
                    return@withContext Result.failure(Exception("Input file does not exist"))
                }
                
                // Create output directory if it doesn't exist
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                // Generate output filename
                val outputFileName = "page_${UUID.randomUUID()}.jpg"
                val outputFile = File(outputDir, outputFileName)
                
                // Load and process the image
                val originalBitmap = BitmapFactory.decodeFile(inputPath)
                    ?: return@withContext Result.failure(Exception("Failed to decode image"))
                
                // Get image orientation
                val orientation = getImageOrientation(inputPath)
                
                // Rotate image if needed
                val rotatedBitmap = rotateImage(originalBitmap, orientation)
                
                // Resize image if too large
                val resizedBitmap = resizeImage(rotatedBitmap, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT)
                
                // Save processed image
                val success = saveBitmap(resizedBitmap, outputFile, JPEG_QUALITY)
                
                // Clean up bitmaps
                if (originalBitmap != resizedBitmap) originalBitmap.recycle()
                if (rotatedBitmap != resizedBitmap) rotatedBitmap.recycle()
                resizedBitmap.recycle()
                
                if (success) {
                    Result.success(
                        ProcessedImageResult(
                            outputPath = outputFile.absolutePath,
                            filename = outputFileName,
                            originalSize = Pair(originalBitmap.width, originalBitmap.height),
                            processedSize = Pair(resizedBitmap.width, resizedBitmap.height),
                            rotationApplied = orientation != 0f
                        )
                    )
                } else {
                    Result.failure(Exception("Failed to save processed image"))
                }
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Batch processes multiple imported images
     * @param inputPaths List of paths to original image files
     * @param outputDir Directory to save processed images
     * @return List of ProcessedImageResult for each processed image
     */
    suspend fun batchProcessImages(
        inputPaths: List<String>,
        outputDir: File
    ): Result<List<ProcessedImageResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<ProcessedImageResult>()
                
                inputPaths.forEach { inputPath ->
                    processImportedImage(inputPath, outputDir)
                        .onSuccess { result ->
                            results.add(result)
                        }
                        .onFailure { exception ->
                            // Log error but continue processing other images
                            System.err.println("Failed to process image $inputPath: ${exception.message}")
                        }
                }
                
                if (results.isEmpty()) {
                    Result.failure(Exception("No images were successfully processed"))
                } else {
                    Result.success(results)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Gets the orientation of an image from EXIF data
     * @param imagePath Path to the image file
     * @return Rotation angle in degrees
     */
    private fun getImageOrientation(imagePath: String): Float {
        return try {
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: IOException) {
            0f
        }
    }
    
    /**
     * Rotates a bitmap by the specified angle
     * @param bitmap The bitmap to rotate
     * @param angle The rotation angle in degrees
     * @return Rotated bitmap
     */
    private fun rotateImage(bitmap: Bitmap, angle: Float): Bitmap {
        return if (angle != 0f) {
            val matrix = Matrix().apply {
                postRotate(angle)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Resizes a bitmap to fit within the specified dimensions while maintaining aspect ratio
     * @param bitmap The bitmap to resize
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Resized bitmap
     */
    private fun resizeImage(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calculate scale factor
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height,
            1f // Don't upscale
        )
        
        return if (scale < 1f) {
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Saves a bitmap to a file with the specified quality
     * @param bitmap The bitmap to save
     * @param file The output file
     * @param quality JPEG quality (0-100)
     * @return True if successful
     */
    private fun saveBitmap(bitmap: Bitmap, file: File, quality: Int): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates if an image file is suitable for processing
     * @param imagePath Path to the image file
     * @return True if image is valid
     */
    suspend fun validateImageFile(imagePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                if (!file.exists() || file.length() == 0L) {
                    return@withContext false
                }
                
                // Try to decode the image
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)
                
                // Check if we got valid dimensions
                options.outWidth > 0 && options.outHeight > 0
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Gets image dimensions without loading the full bitmap
     * @param imagePath Path to the image file
     * @return Pair of width and height, or null if failed
     */
    suspend fun getImageDimensions(imagePath: String): Pair<Int, Int>? {
        return withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)
                
                if (options.outWidth > 0 && options.outHeight > 0) {
                    Pair(options.outWidth, options.outHeight)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Cleans up temporary processed images
     * @param directory Directory containing temporary files
     */
    fun cleanupProcessedImages(directory: File) {
        try {
            if (directory.exists()) {
                directory.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".jpg")) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Result data class for processed image information
 */
data class ProcessedImageResult(
    val outputPath: String,
    val filename: String,
    val originalSize: Pair<Int, Int>,
    val processedSize: Pair<Int, Int>,
    val rotationApplied: Boolean
)

/**
 * Configuration for image processing
 */
data class ImageProcessingConfig(
    val maxWidth: Int = 1920,
    val maxHeight: Int = 1080,
    val jpegQuality: Int = 85,
    val autoRotate: Boolean = true
)