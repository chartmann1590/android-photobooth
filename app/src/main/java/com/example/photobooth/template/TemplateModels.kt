package com.example.photobooth.template

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.content.Context
import android.graphics.BitmapFactory

data class FrameSlot(
    val leftPercent: Float,
    val topPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float,
)

data class TextOverlay(
    val text: String,
    val xPercent: Float,
    val yPercent: Float,
    val textSizeSp: Float,
)

data class TemplateDefinition(
    val widthPx: Int,
    val heightPx: Int,
    val backgroundColor: Int = Color.BLACK,
    val backgroundImagePath: String? = null,
    val frames: List<FrameSlot>,
    val overlays: List<TextOverlay>,
)

/**
 * Center-crops [source] to fill [targetW] x [targetH], scaling up/down as needed.
 */
fun centerCropToFill(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
    val srcRatio = source.width.toFloat() / source.height
    val dstRatio = targetW.toFloat() / targetH

    val (scaledW, scaledH) = if (srcRatio > dstRatio) {
        // source is wider → match height, crop width
        val h = targetH
        val w = (source.width.toFloat() * targetH / source.height).toInt()
        w to h
    } else {
        // source is taller → match width, crop height
        val w = targetW
        val h = (source.height.toFloat() * targetW / source.width).toInt()
        w to h
    }

    val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
    val x = (scaledW - targetW) / 2
    val y = (scaledH - targetH) / 2
    return Bitmap.createBitmap(scaled, x, y, targetW, targetH)
}

/** Portrait 4x6 at 300 DPI */
const val OUTPUT_4X6_WIDTH = 1200
const val OUTPUT_4X6_HEIGHT = 1800

/**
 * Renders a captured photo as a 4x6 portrait (1200x1800) JPEG.
 * The input bitmap must already be EXIF-corrected.
 * Center-crops to fill, then composites frame overlay on top if present.
 */
fun renderSimple4x6(capturedBitmap: Bitmap, frameOverlayPath: String?): Bitmap {
    val cropped = centerCropToFill(capturedBitmap, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
    if (frameOverlayPath == null) return cropped

    val output = cropped.copy(Bitmap.Config.ARGB_8888, true)
    cropped.recycle()
    val canvas = Canvas(output)
    val overlay = BitmapFactory.decodeFile(frameOverlayPath)
    if (overlay != null) {
        val dest = Rect(0, 0, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
        canvas.drawBitmap(overlay, null, dest, null)
        overlay.recycle()
    }
    return output
}

class TemplateRenderer(
    private val context: Context,
) {
    fun render(
        template: TemplateDefinition,
        capturedBitmaps: List<Bitmap>,
    ): Bitmap {
        val output = Bitmap.createBitmap(
            template.widthPx,
            template.heightPx,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)

        canvas.drawColor(template.backgroundColor)
        template.backgroundImagePath?.let { path ->
            val bgBitmap = BitmapFactory.decodeFile(path)
            bgBitmap?.let {
                val dest = Rect(0, 0, template.widthPx, template.heightPx)
                canvas.drawBitmap(it, null, dest, null)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        template.frames.forEachIndexed { index, frame ->
            val bmp = capturedBitmaps.getOrNull(index) ?: return@forEachIndexed
            val left = (frame.leftPercent * template.widthPx).toInt()
            val top = (frame.topPercent * template.heightPx).toInt()
            val width = (frame.widthPercent * template.widthPx).toInt()
            val height = (frame.heightPercent * template.heightPx).toInt()
            val dest = Rect(left, top, left + width, top + height)
            canvas.drawBitmap(bmp, null, dest, null)
        }

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        template.overlays.forEach { overlay ->
            paint.textSize = overlay.textSizeSp * context.resources.displayMetrics.scaledDensity
            val x = overlay.xPercent * template.widthPx
            val y = overlay.yPercent * template.heightPx
            canvas.drawText(overlay.text, x, y, paint)
        }

        return output
    }
}

