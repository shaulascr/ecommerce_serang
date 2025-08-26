package com.alya.ecommerce_serang.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
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

    // for Uri format
//    fun compressFileToMax1MB(context: Context, uri: Uri): File? {
//        val mimeType = context.contentResolver.getType(uri)
//
//        return if (mimeType?.startsWith("image/") == true) {
//            // Handle images (jpg/png)
//            compressImageToMax1MB(context, uri)
//        } else if (mimeType == "application/pdf") {
//            // Handle PDFs
//            val file = createTempFileFromUri(context, uri, "pdf")
//            return if (file != null && file.length() <= 1_048_576) {
//                file
//            } else {
//                // 🚨 Without a PDF compression lib, you can only reject if > 1 MB
//                null
//            }
//        } else {
//            // Unsupported type
//            null
//        }
//    }

    fun compressFileToMax1MB(context: Context, uri: Uri): CompressionResult {
        val mimeType = context.contentResolver.getType(uri) ?: return CompressionResult.Error("Tipe file tidak diketahui")

        return if (mimeType.startsWith("image/")) {
            val compressed = compressImageToMax1MB(context, uri)
            if (compressed != null) {
                CompressionResult.Success(compressed)
            } else {
                CompressionResult.Error("Ukuran gambar terlalu besar. Max 1MB.")
            }
        } else if (mimeType == "application/pdf") {
            val file = createTempFileFromUri(context, uri, "pdf")
            if (file == null) {
                return CompressionResult.Error("Tidak bisa membaca file pdf")
            }
            if (file.length() <= 1_048_576) {
                return CompressionResult.Success(file)
            }
            val compressed = compressPdfToMax1MB(context, file)
            if (compressed != null) {
                CompressionResult.Success(compressed)
            } else {
                CompressionResult.Error("Ukuran pdf terlalu besar. Max 1MB.")
            }
        } else {
            CompressionResult.Error("Tipe file tidak didukung: $mimeType")
        }
    }

    fun compressImageToMax1MB(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        var quality = 100
        var compressedFile: File
        var outputStream: ByteArrayOutputStream

        do {
            outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            val compressedBytes = outputStream.toByteArray()
            compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { it.write(compressedBytes) }

            quality -= 5
        } while (compressedFile.length() > 1_048_576 && quality > 10)

        return if (compressedFile.length() <= 1_048_576) compressedFile else null
    }

    fun compressPdfToMax1MB(context: Context, inputFile: File): File? {
        return try {
            val document = PDDocument.load(inputFile)
            val renderer = PDFRenderer(document)

            val compressedDoc = PDDocument()

            for (pageIndex in 0 until document.numberOfPages) {
                val bitmap = renderer.renderImageWithDPI(pageIndex, 72f) // low DPI → smaller size
                val outPage = com.tom_roush.pdfbox.pdmodel.PDPage(
                    com.tom_roush.pdfbox.pdmodel.common.PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat())
                )
                compressedDoc.addPage(outPage)

                val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(compressedDoc, bitmap)
                val contentStream = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(compressedDoc, outPage)
                contentStream.drawImage(pdImage, 0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                contentStream.close()
            }

            val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.pdf")
            compressedDoc.save(compressedFile)
            compressedDoc.close()
            document.close()

            // Check size
            return if (compressedFile.length() <= 1_048_576) compressedFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

sealed class CompressionResult {
    data class Success(val file: File) : CompressionResult()
    data class Error(val reason: String) : CompressionResult()
}