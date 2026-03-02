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

