package com.alya.ecommerce_serang.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.min

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_WIDTH = 1024
    private const val MAX_HEIGHT = 1024
    private const val QUALITY = 80

    /**
     * Compresses an image from a Uri
     *
     * @param context The context
     * @param uri The URI of the image to compress
     * @param maxWidth Maximum width (default 1024px)
     * @param maxHeight Maximum height (default 1024px)
     * @param quality JPEG quality (0-100, default 80)
     * @return A File containing the compressed image
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        filename: String,
        maxWidth: Int = MAX_WIDTH,
        maxHeight: Int = MAX_HEIGHT,
        quality: Int = QUALITY
    ): File {
        Log.d(TAG, "Starting image compression for $filename")

        // Create input stream and decode the original bitmap
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        val mimeType = options.outMimeType ?: "image/jpeg"

        Log.d(TAG, "Original size: ${originalWidth}x${originalHeight}, mime: $mimeType")

        // Calculate inSampleSize based on required dimensions
        val inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)

        // Open a new input stream since we closed the previous one
        val newInputStream = context.contentResolver.openInputStream(uri)

        // Decode with actual sampling
        val decodingOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val sampledBitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
            ?: throw IllegalArgumentException("Failed to decode bitmap from URI")
        newInputStream?.close()

        Log.d(TAG, "Decoded size: ${sampledBitmap.width}x${sampledBitmap.height}, sample size: $inSampleSize")

        // Create output file
        val extension = when {
            mimeType.contains("png") -> ".png"
            mimeType.contains("webp") -> ".webp"
            else -> ".jpg"
        }
        val outputFile = File(context.cacheDir, "${filename}_${System.currentTimeMillis()}$extension")

        // Scale if still needed (in case inSampleSize couldn't get exact dimensions)
        val scaledBitmap = if (sampledBitmap.width > maxWidth || sampledBitmap.height > maxHeight) {
            val widthRatio = maxWidth.toFloat() / sampledBitmap.width
            val heightRatio = maxHeight.toFloat() / sampledBitmap.height
            val scaleFactor = min(widthRatio, heightRatio)

            val scaledWidth = (sampledBitmap.width * scaleFactor).toInt()
            val scaledHeight = (sampledBitmap.height * scaleFactor).toInt()

            Log.d(TAG, "Scaling to: ${scaledWidth}x${scaledHeight}")
            Bitmap.createScaledBitmap(sampledBitmap, scaledWidth, scaledHeight, true)
        } else {
            sampledBitmap
        }

        // Save to file with compression
        val format = when {
            mimeType.contains("png") -> Bitmap.CompressFormat.PNG
            mimeType.contains("webp") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ->
                Bitmap.CompressFormat.WEBP_LOSSY
            mimeType.contains("webp") -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }

        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(format, quality, out)
            out.flush()
        }

        // Clean up
        if (scaledBitmap != sampledBitmap) {
            scaledBitmap.recycle()
        }
        sampledBitmap.recycle()

        val originalSize = getUriFileSize(context, uri)
        val compressedSize = outputFile.length()

        Log.d(TAG, "Compression complete. Original size: ${originalSize/1024}KB, " +
                "Compressed size: ${compressedSize/1024}KB, " +
                "Reduction: ${(100 - (compressedSize * 100 / originalSize))}%")

        return outputFile
    }

    /**
     * Calculate the optimal inSampleSize value for bitmap downsampling
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, maxWidth: Int, maxHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > maxHeight || width > maxWidth) {
            val heightRatio = Math.round(height.toFloat() / maxHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / maxWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        // Ensure power of 2 for better performance
        var powerOf2 = 1
        while (powerOf2 * 2 <= inSampleSize) {
            powerOf2 *= 2
        }

        return powerOf2
    }

    /**
     * Get the file size from a Uri
     */
    private fun getUriFileSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)
        cursor?.moveToFirst()

        val size = if (sizeIndex != null && sizeIndex >= 0) {
            cursor.getLong(sizeIndex)
        } else {
            // If size can't be determined from cursor, read stream length
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        }

        cursor?.close()
        return size
    }

    fun isAllowedFileType(context: Context, uri: Uri?, allowedTypes: Regex): Boolean {
        if (uri == null) return false

        val mimeType = context.contentResolver.getType(uri) ?: ""
        Log.d(TAG, "Checking file type: $mimeType")

        // Get file extension from mime type
        val extension = when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("png") -> "png"
            mimeType.contains("pdf") -> "pdf"
            else -> {
                // If mime type is not helpful, try to get extension from URI
                val fileName = uri.path?.substringAfterLast('/') ?: ""
                fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            }
        }

        val isAllowed = allowedTypes.matches(extension)
        Log.d(TAG, "File extension: $extension, Allowed: $isAllowed")

        return isAllowed
    }
}