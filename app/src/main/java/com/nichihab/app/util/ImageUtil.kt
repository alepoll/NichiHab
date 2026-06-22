package com.nichihab.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageUtil {

    /**
     * Resizes a bitmap so the longest edge is at most [maxEdge] pixels,
     * then JPEG-compresses and Base64-encodes it.
     */
    fun bitmapToBase64(bitmap: Bitmap, maxEdge: Int = Constants.MAX_IMAGE_EDGE_PX, quality: Int = 85): String {
        val resized = resizeBitmap(bitmap, maxEdge)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        if (resized != bitmap) resized.recycle()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Checks average luminance of a bitmap.
     * Returns a value 0..255 where < 50 is considered too dark.
     */
    fun checkLuminance(bitmap: Bitmap): Int {
        val scaledDown = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        var totalLuminance = 0L
        val width = scaledDown.width
        val height = scaledDown.height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = scaledDown.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Perceived luminance formula
                totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
            }
        }
        scaledDown.recycle()
        return (totalLuminance / (width * height)).toInt()
    }

    /**
     * Checks image sharpness using Laplacian Variance.
     * Returns variance of the Laplacian-filtered image.
     * Values below [Constants.BLUR_THRESHOLD] indicate a blurry photo.
     *
     * Algorithm:
     * 1. Downscale to 256x256 grayscale
     * 2. Apply 3x3 Laplacian kernel convolution
     * 3. Compute variance of the result
     */
    fun checkSharpness(bitmap: Bitmap): Double {
        val size = 256
        // Downscale
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)

        // Convert to grayscale pixel array
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        scaled.recycle()

        val gray = DoubleArray(size * size) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            0.299 * r + 0.587 * g + 0.114 * b
        }

        // Laplacian kernel: [0, 1, 0; 1, -4, 1; 0, 1, 0]
        val laplacian = DoubleArray(size * size)
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val idx = y * size + x
                laplacian[idx] = gray[idx - size] + gray[idx + size] +
                        gray[idx - 1] + gray[idx + 1] - 4.0 * gray[idx]
            }
        }

        // Compute variance of the Laplacian
        val count = ((size - 2) * (size - 2)).toDouble()
        var sum = 0.0
        var sumSq = 0.0
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val v = laplacian[y * size + x]
                sum += v
                sumSq += v * v
            }
        }
        val mean = sum / count
        return (sumSq / count) - (mean * mean)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longestEdge = max(bitmap.width, bitmap.height)
        if (longestEdge <= maxEdge) return bitmap

        val scale = maxEdge.toFloat() / longestEdge
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Decodes a byte array to Bitmap.
     */
    fun byteArrayToBitmap(data: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }
}
