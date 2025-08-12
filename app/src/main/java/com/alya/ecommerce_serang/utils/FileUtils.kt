package com.alya.ecommerce_serang.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Compress a file to GZIP format to reduce its size to below 1MB
     * @param context The context
     * @param uri The URI of the file to compress
     * @param maxSize The target size limit in bytes (1MB = 1048576 bytes)
     * @return The compressed file, or null if compression failed
     */
    fun compressFile(context: Context, uri: Uri, maxSize: Long = 1048576L): File? {
        try {
            // Create a temporary file for compressed content
            val originalFile = createTempFileFromUri(context, uri, "compressed")
            val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.gz")

            // Compress the original file into the GZIP file
            compressToGZIP(originalFile, compressedFile)

            // Check if the compressed file is larger than the allowed size
            if (compressedFile.length() <= maxSize) {
                Log.d(TAG, "Compression successful. Compressed file size: ${compressedFile.length()} bytes.")
                return compressedFile
            } else {
                // If the file is still too large, you can handle it by reducing quality or adjusting compression logic
                Log.e(TAG, "Compressed file exceeds the size limit. Size: ${compressedFile.length()} bytes.")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during file compression: ${e.message}", e)
            return null
        }
    }

    /**
     * Creates a temporary file from the URI in the app's cache directory.
     */
    fun createTempFileFromUri(context: Context, uri: Uri, prefix: String = "temp"): File? {
        try {
            val fileExtension = getFileExtension(context, uri)
            val fileName = "${prefix}_${System.currentTimeMillis()}.$fileExtension"
            val tempFile = File(context.cacheDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return if (tempFile.exists() && tempFile.length() > 0) {
                Log.d(TAG, "Created temp file: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
                tempFile
            } else {
                Log.e(TAG, "Created file is empty or doesn't exist")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file: ${e.message}", e)
            return null
        }
    }

    /**
     * Compress the input file into a GZIP file.
     */
    private fun compressToGZIP(inputFile: File?, outputFile: File) {
        FileInputStream(inputFile).use { inputStream ->
            FileOutputStream(outputFile).use { fileOutputStream ->
                GZIPOutputStream(fileOutputStream).use { gzipOutputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        gzipOutputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }

    /**
     * Gets the file extension from a URI using ContentResolver
     */
    fun getFileExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return if (mimeType != null) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        } else {
            // Try to extract from the URI path
            val path = uri.toString()
            if (path.contains(".")) {
                path.substring(path.lastIndexOf(".") + 1)
            } else {
                "jpg" // Default extension
            }
        }
    }

    /**
     * Creates a MultipartBody.Part from a File for API requests
     */
    fun createMultipartFromFile(paramName: String, file: File): MultipartBody.Part {
        val requestFile = file.asRequestBody(getMimeType(file).toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(paramName, file.name, requestFile)
    }

    /**
     * Creates an empty MultipartBody.Part
     */
    fun createEmptyMultipart(paramName: String): MultipartBody.Part {
        return MultipartBody.Part.createFormData(paramName, "")
    }

    /**
     * Gets the MIME type for a file based on its extension
     */
    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }
}