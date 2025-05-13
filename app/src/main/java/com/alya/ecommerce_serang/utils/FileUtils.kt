package com.alya.ecommerce_serang.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Creates a temporary file from a URI in the app's cache directory
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