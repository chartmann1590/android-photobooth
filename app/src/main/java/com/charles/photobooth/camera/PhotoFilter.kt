package com.charles.photobooth.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class PhotoFilter(val displayName: String) {
    NONE("Original"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    BLACK_AND_WHITE("B&W"),
    VINTAGE("Vintage"),
    COOL("Cool"),
    WARM("Warm"),
    VIVID("Vivid"),
}

fun applyFilter(bitmap: Bitmap, filter: PhotoFilter): Bitmap {
    if (filter == PhotoFilter.NONE) return bitmap

    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    when (filter) {
        PhotoFilter.GRAYSCALE -> {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        PhotoFilter.SEPIA -> {
            val matrix = ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        PhotoFilter.BLACK_AND_WHITE -> {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            val contrast = ColorMatrix(floatArrayOf(
                1.5f, 0f, 0f, 0f, -128f,
                0f, 1.5f, 0f, 0f, -128f,
                0f, 0f, 1.5f, 0f, -128f,
                0f, 0f, 0f, 1f, 0f,
            ))
            matrix.postConcat(contrast)
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        PhotoFilter.VINTAGE -> {
            val matrix = ColorMatrix(floatArrayOf(
                0.6f, 0.3f, 0.1f, 0f, 20f,
                0.2f, 0.6f, 0.1f, 0f, 10f,
                0.1f, 0.2f, 0.5f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        PhotoFilter.COOL -> {
            val matrix = ColorMatrix(floatArrayOf(
                0.8f, 0f, 0f, 0f, 0f,
                0f, 0.9f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f,
            ))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        PhotoFilter.WARM -> {
            val matrix = ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 10f,
                0f, 1.0f, 0f, 0f, 0f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ))
            paint.colorFilter = ColorMatrixColorFilter(matrix)
        }
        PhotoFilter.VIVID -> {
            val sat = ColorMatrix().apply { setSaturation(1.6f) }
            val contrast = ColorMatrix(floatArrayOf(
                1.1f, 0f, 0f, 0f, 5f,
                0f, 1.1f, 0f, 0f, 5f,
                0f, 0f, 1.1f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f,
            ))
            sat.postConcat(contrast)
            paint.colorFilter = ColorMatrixColorFilter(sat)
        }
        PhotoFilter.NONE -> {}
    }

    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return output
}
