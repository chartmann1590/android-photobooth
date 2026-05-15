package com.charles.photobooth.template

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

fun centerCropToFill(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
    val srcRatio = source.width.toFloat() / source.height
    val dstRatio = targetW.toFloat() / targetH

    val (scaledW, scaledH) = if (srcRatio > dstRatio) {
        val h = targetH
        val w = (source.width.toFloat() * targetH / source.height).toInt()
        w to h
    } else {
        val w = targetW
        val h = (source.height.toFloat() * targetW / source.width).toInt()
        w to h
    }

    val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
    val x = (scaledW - targetW) / 2
    val y = (scaledH - targetH) / 2
    val result = Bitmap.createBitmap(scaled, x, y, targetW, targetH)
    if (scaled !== source && scaled !== result) scaled.recycle()
    return result
}

const val OUTPUT_4X6_WIDTH = 1200
const val OUTPUT_4X6_HEIGHT = 1800

enum class WatermarkPosition {
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    TOP_RIGHT,
    TOP_LEFT,
    TOP_CENTER,
}

data class WatermarkConfig(
    val imagePath: String = "",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val sizePercent: Float = 0.15f,
    val opacity: Int = 180,
)

fun applyWatermark(bitmap: Bitmap, watermark: WatermarkConfig): Bitmap {
    if (watermark.imagePath.isBlank()) return bitmap
    val logo = BitmapFactory.decodeFile(watermark.imagePath) ?: return bitmap
    val output = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return bitmap
    val canvas = Canvas(output)

    val logoW = (output.width * watermark.sizePercent).toInt()
    val aspect = logo.height.toFloat() / logo.width
    val logoH = (logoW * aspect).toInt()
    val margin = (output.width * 0.03f).toInt()

    val left = when (watermark.position) {
        WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.TOP_LEFT -> margin
        WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.TOP_CENTER -> (output.width - logoW) / 2
        WatermarkPosition.BOTTOM_RIGHT, WatermarkPosition.TOP_RIGHT -> output.width - logoW - margin
    }
    val top = when (watermark.position) {
        WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT, WatermarkPosition.TOP_CENTER -> margin
        WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT, WatermarkPosition.BOTTOM_CENTER -> output.height - logoH - margin
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = watermark.opacity }
    canvas.drawBitmap(logo, null, Rect(left, top, left + logoW, top + logoH), paint)
    logo.recycle()
    return output
}

fun renderSimple4x6(capturedBitmap: Bitmap, frameOverlayPath: String?, watermark: WatermarkConfig? = null): Bitmap {
    val cropped = centerCropToFill(capturedBitmap, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
    val composited = if (frameOverlayPath == null) {
        cropped
    } else {
        val output = cropped.copy(Bitmap.Config.ARGB_8888, true)
        cropped.recycle()
        val canvas = Canvas(output)
        val overlay = BitmapFactory.decodeFile(frameOverlayPath)
        if (overlay != null) {
            try {
                val dest = Rect(0, 0, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
                canvas.drawBitmap(overlay, null, dest, null)
            } finally {
                overlay.recycle()
            }
        }
        output
    }
    if (watermark != null && watermark.imagePath.isNotBlank()) {
        val watermarked = applyWatermark(composited, watermark)
        if (watermarked !== composited) composited.recycle()
        return watermarked
    }
    return composited
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
                try {
                    val dest = Rect(0, 0, template.widthPx, template.heightPx)
                    canvas.drawBitmap(it, null, dest, null)
                } finally {
                    it.recycle()
                }
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        template.frames.forEachIndexed { index, frame ->
            val bmp = capturedBitmaps.getOrNull(index) ?: return@forEachIndexed
            val left = (frame.leftPercent * template.widthPx).toInt()
            val top = (frame.topPercent * template.heightPx).toInt()
            val width = (frame.widthPercent * template.widthPx).toInt().coerceAtLeast(1)
            val height = (frame.heightPercent * template.heightPx).toInt().coerceAtLeast(1)
            val dest = Rect(left, top, left + width, top + height)

            // Center-crop the source bitmap to the destination's aspect ratio so faces
            // don't stretch when the slot's aspect differs from the photo's (e.g., a
            // 2:3 photo into a near-square 2x2 cell).
            val srcAspect = bmp.width.toFloat() / bmp.height.toFloat()
            val dstAspect = width.toFloat() / height.toFloat()
            val srcRect = if (srcAspect > dstAspect) {
                val srcWidth = (bmp.height * dstAspect).toInt().coerceAtLeast(1)
                val srcLeft = (bmp.width - srcWidth) / 2
                Rect(srcLeft, 0, srcLeft + srcWidth, bmp.height)
            } else {
                val srcHeight = (bmp.width / dstAspect).toInt().coerceAtLeast(1)
                val srcTop = (bmp.height - srcHeight) / 2
                Rect(0, srcTop, bmp.width, srcTop + srcHeight)
            }
            canvas.drawBitmap(bmp, srcRect, dest, null)
        }

        val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            // setShadowLayer makes white captions readable even when the band color is
            // close to white in the photo content. ~3% blur of canvas height looks crisp.
            setShadowLayer(template.heightPx * 0.004f, 0f, template.heightPx * 0.002f, Color.BLACK)
        }

        // Scale text size relative to the canvas, not device DPI. 14sp -> ~5% of canvas
        // height, so an 18sp title at 1800px is ~115px tall and clearly readable.
        val textScale = template.heightPx * 0.0036f
        // Reserve a 4% margin on each side so text never touches the canvas edge.
        val maxTextWidth = template.widthPx * 0.92f
        template.overlays.forEach { overlay ->
            val baseSize = overlay.textSizeSp * textScale
            textPaint.textSize = baseSize
            val measuredAtBase = textPaint.measureText(overlay.text)

            // Shrink-to-fit: drop the font size proportionally until the text fits.
            // Floor at 60% of base so the text stays readable. If even the floor
            // overflows, ellipsize so the text never bleeds past the canvas.
            val displayText: String = if (measuredAtBase <= maxTextWidth) {
                overlay.text
            } else {
                val target = (baseSize * maxTextWidth / measuredAtBase)
                    .coerceAtLeast(baseSize * 0.6f)
                textPaint.textSize = target
                val measuredAtTarget = textPaint.measureText(overlay.text)
                if (measuredAtTarget <= maxTextWidth) {
                    overlay.text
                } else {
                    android.text.TextUtils.ellipsize(
                        overlay.text,
                        textPaint,
                        maxTextWidth,
                        android.text.TextUtils.TruncateAt.END,
                    ).toString()
                }
            }

            val x = overlay.xPercent * template.widthPx
            val y = overlay.yPercent * template.heightPx
            canvas.drawText(displayText, x, y, textPaint)
        }

        return output
    }
}

object BuiltInTemplates {
    fun singlePortrait(eventName: String) = TemplateDefinition(
        widthPx = OUTPUT_4X6_WIDTH,
        heightPx = OUTPUT_4X6_HEIGHT,
        frames = listOf(FrameSlot(0.05f, 0.05f, 0.9f, 0.75f)),
        overlays = listOf(
            TextOverlay(eventName, 0.5f, 0.88f, 14f),
        ),
    )

    fun photoStrip2x2(eventName: String) = TemplateDefinition(
        widthPx = OUTPUT_4X6_WIDTH,
        heightPx = OUTPUT_4X6_HEIGHT,
        frames = listOf(
            FrameSlot(0.05f, 0.03f, 0.43f, 0.45f),
            FrameSlot(0.52f, 0.03f, 0.43f, 0.45f),
            FrameSlot(0.05f, 0.51f, 0.43f, 0.45f),
            FrameSlot(0.52f, 0.51f, 0.43f, 0.45f),
        ),
        overlays = listOf(
            TextOverlay(eventName, 0.5f, 0.98f, 10f),
        ),
    )

    fun photoStripVertical(eventName: String) = TemplateDefinition(
        widthPx = OUTPUT_4X6_WIDTH,
        heightPx = OUTPUT_4X6_HEIGHT,
        frames = listOf(
            FrameSlot(0.1f, 0.02f, 0.8f, 0.3f),
            FrameSlot(0.1f, 0.35f, 0.8f, 0.3f),
            FrameSlot(0.1f, 0.68f, 0.8f, 0.3f),
        ),
        overlays = listOf(
            TextOverlay(eventName, 0.5f, 0.99f, 10f),
        ),
    )

    private fun eventPreset(
        title: String,
        date: String,
        backgroundColor: Int,
    ) = TemplateDefinition(
        widthPx = OUTPUT_4X6_WIDTH,
        heightPx = OUTPUT_4X6_HEIGHT,
        backgroundColor = backgroundColor,
        // Photo at the top with a fat caption band at the bottom. 8% margin on the
        // sides + 4% top, photo height 72% (ends at y=0.76), leaving 24% (~430px) of
        // band -- enough room for a 24sp title above a 14sp date without overlap.
        frames = listOf(FrameSlot(0.08f, 0.04f, 0.84f, 0.72f)),
        overlays = buildList {
            add(TextOverlay(title, 0.5f, 0.87f, 24f))
            if (date.isNotBlank()) {
                add(TextOverlay(date, 0.5f, 0.95f, 14f))
            }
        },
    )

    fun birthday(title: String, date: String) =
        eventPreset(title, date, 0xFFB23A5E.toInt())

    fun wedding(title: String, date: String) =
        eventPreset(title, date, 0xFF3A4A6B.toInt())

    fun anniversary(title: String, date: String) =
        eventPreset(title, date, 0xFF7A1F3D.toInt())

    fun holiday(title: String, date: String) =
        eventPreset(title, date, 0xFF1F5C3A.toInt())

    fun generic(title: String, date: String) =
        eventPreset(title, date, 0xFF1E2738.toInt())

    const val KEY_NONE = "NONE"
    const val KEY_SINGLE = "SINGLE"
    const val KEY_STRIP_2X2 = "STRIP_2x2"
    const val KEY_STRIP_VERTICAL = "STRIP_VERTICAL"
    const val KEY_BIRTHDAY = "EVENT_BIRTHDAY"
    const val KEY_WEDDING = "EVENT_WEDDING"
    const val KEY_ANNIVERSARY = "EVENT_ANNIVERSARY"
    const val KEY_HOLIDAY = "EVENT_HOLIDAY"
    const val KEY_GENERIC = "EVENT_GENERIC"

    fun fromKey(key: String, eventName: String, eventDate: String = ""): TemplateDefinition? = when (key) {
        KEY_STRIP_2X2 -> photoStrip2x2(eventName)
        KEY_STRIP_VERTICAL -> photoStripVertical(eventName)
        KEY_BIRTHDAY -> birthday(eventName, eventDate)
        KEY_WEDDING -> wedding(eventName, eventDate)
        KEY_ANNIVERSARY -> anniversary(eventName, eventDate)
        KEY_HOLIDAY -> holiday(eventName, eventDate)
        KEY_GENERIC -> generic(eventName, eventDate)
        else -> null
    }
}
