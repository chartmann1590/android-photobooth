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

// Alpha value below which a frame overlay pixel is considered "reveals the photo below".
private const val FRAME_WINDOW_ALPHA_THRESHOLD = 32
// Frame overlays without a meaningfully-sized transparent window (e.g. a corner logo/watermark
// style overlay) fall back to the old full-bleed behavior instead of being treated as a window.
private const val FRAME_WINDOW_MIN_AREA_FRACTION = 0.15f

/**
 * Finds the bounding box of the mostly-transparent "photo window" in a frame overlay, in the
 * overlay's own pixel coordinates. Returns null if the overlay has no window worth treating
 * specially (e.g. a decorative full-bleed graphic or small watermark).
 *
 * This flood-fills outward from the image center rather than taking a bounding box of every
 * low-alpha pixel in the image. A plain bounding box is fragile: a handful of stray
 * anti-aliased pixels anywhere else in the artwork (soft edges on text, thin gold linework)
 * can dip under the alpha threshold and balloon the box far beyond the real window - which
 * showed up as the frame's title/caption bands sometimes vanishing under the photo. Only
 * pixels reachable from the center through other window pixels count.
 */
private fun detectFrameWindow(frame: Bitmap): Rect? {
    val w = frame.width
    val h = frame.height
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    frame.getPixels(pixels, 0, w, 0, 0, w, h)

    fun isWindowPixel(index: Int): Boolean {
        val alpha = (pixels[index] ushr 24) and 0xFF
        return alpha <= FRAME_WINDOW_ALPHA_THRESHOLD
    }

    val startX = w / 2
    val startY = h / 2
    val startIndex = startY * w + startX
    if (!isWindowPixel(startIndex)) return null

    val visited = BooleanArray(w * h)
    val stack = IntArray(w * h)
    var stackSize = 0
    stack[stackSize++] = startIndex
    visited[startIndex] = true

    var minX = startX
    var maxX = startX
    var minY = startY
    var maxY = startY

    while (stackSize > 0) {
        val index = stack[--stackSize]
        val x = index % w
        val y = index / w
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y

        if (x > 0) {
            val n = index - 1
            if (!visited[n] && isWindowPixel(n)) { visited[n] = true; stack[stackSize++] = n }
        }
        if (x < w - 1) {
            val n = index + 1
            if (!visited[n] && isWindowPixel(n)) { visited[n] = true; stack[stackSize++] = n }
        }
        if (y > 0) {
            val n = index - w
            if (!visited[n] && isWindowPixel(n)) { visited[n] = true; stack[stackSize++] = n }
        }
        if (y < h - 1) {
            val n = index + w
            if (!visited[n] && isWindowPixel(n)) { visited[n] = true; stack[stackSize++] = n }
        }
    }

    val windowArea = (maxX - minX + 1).toLong() * (maxY - minY + 1).toLong()
    val totalArea = w.toLong() * h.toLong()
    if (windowArea.toFloat() / totalArea < FRAME_WINDOW_MIN_AREA_FRACTION) return null

    return Rect(minX, minY, maxX + 1, maxY + 1)
}

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
    val overlay = frameOverlayPath?.let { BitmapFactory.decodeFile(it) }
    val window = overlay?.let { detectFrameWindow(it) }

    val composited = if (overlay == null) {
        centerCropToFill(capturedBitmap, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
    } else if (window == null) {
        // No usable transparent window in the overlay: keep the old full-bleed behavior
        // (photo fills the whole canvas, overlay art drawn on top).
        val cropped = centerCropToFill(capturedBitmap, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
        val output = cropped.copy(Bitmap.Config.ARGB_8888, true)
        cropped.recycle()
        val canvas = Canvas(output)
        try {
            canvas.drawBitmap(overlay, null, Rect(0, 0, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT), null)
        } finally {
            overlay.recycle()
        }
        output
    } else {
        // The overlay has a real photo window: fit the photo to that window's own aspect
        // ratio and position instead of the full canvas. Previously the photo was always
        // cropped to fill the entire 1200x1800 canvas and the frame's opaque border was
        // drawn on top, which hid a large slice of the (already-cropped) photo behind the
        // border - looking over-zoomed and cutting off people near the edges. Fitting the
        // photo to the actual visible window instead shows as much of the original shot
        // as that window's aspect ratio allows.
        val scaleX = OUTPUT_4X6_WIDTH.toFloat() / overlay.width
        val scaleY = OUTPUT_4X6_HEIGHT.toFloat() / overlay.height
        val destWindow = Rect(
            (window.left * scaleX).toInt(),
            (window.top * scaleY).toInt(),
            (window.right * scaleX).toInt(),
            (window.bottom * scaleY).toInt(),
        )
        val windowW = (destWindow.right - destWindow.left).coerceAtLeast(1)
        val windowH = (destWindow.bottom - destWindow.top).coerceAtLeast(1)
        val fitted = centerCropToFill(capturedBitmap, windowW, windowH)

        val output = Bitmap.createBitmap(OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(fitted, destWindow.left.toFloat(), destWindow.top.toFloat(), null)
        fitted.recycle()
        try {
            canvas.drawBitmap(overlay, null, Rect(0, 0, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT), null)
        } finally {
            overlay.recycle()
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

/**
 * Renders a multi-photo template (2x2 grid, photo strip, ...) the same way [TemplateRenderer]
 * does, but if a decorative overlay frame is selected and has a real photo window, the whole
 * grid is scaled down and placed inside that window - then the frame is drawn on top - instead
 * of the grid covering the full canvas and being covered by the frame's border. Falls back to
 * the plain grid render when there's no overlay, or the overlay has no usable window, or its
 * canvas size doesn't match the template's.
 */
fun renderMultiPhotoTemplate(
    context: Context,
    template: TemplateDefinition,
    capturedBitmaps: List<Bitmap>,
    frameOverlayPath: String?,
): Bitmap {
    val plainRender = { TemplateRenderer(context).render(template, capturedBitmaps) }
    if (frameOverlayPath.isNullOrBlank()) return plainRender()

    val overlay = BitmapFactory.decodeFile(frameOverlayPath) ?: return plainRender()
    val window = detectFrameWindow(overlay)
    if (window == null || overlay.width != template.widthPx || overlay.height != template.heightPx) {
        overlay.recycle()
        return plainRender()
    }

    val windowW = (window.right - window.left).coerceAtLeast(1)
    val windowH = (window.bottom - window.top).coerceAtLeast(1)

    // Each frame slot is defined as a fraction of the full canvas; re-express it as a fraction
    // of the detected window instead, so the whole grid lands inside the window rather than
    // being sized for (and partly hidden behind) the frame's decorative border.
    val remappedFrames = template.frames.map { slot ->
        FrameSlot(
            leftPercent = (window.left + slot.leftPercent * windowW) / template.widthPx.toFloat(),
            topPercent = (window.top + slot.topPercent * windowH) / template.heightPx.toFloat(),
            widthPercent = (slot.widthPercent * windowW) / template.widthPx.toFloat(),
            heightPercent = (slot.heightPercent * windowH) / template.heightPx.toFloat(),
        )
    }
    val remappedTemplate = template.copy(frames = remappedFrames, backgroundImagePath = null)

    val output = TemplateRenderer(context).render(remappedTemplate, capturedBitmaps)
    val canvas = Canvas(output)
    try {
        canvas.drawBitmap(overlay, null, Rect(0, 0, template.widthPx, template.heightPx), null)
    } finally {
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

            if (measuredAtBase > maxTextWidth) {
                // Keep shrinking until it fits
                var target = baseSize * (maxTextWidth / measuredAtBase)
                textPaint.textSize = target
                while (textPaint.measureText(overlay.text) > maxTextWidth && target > 10f) {
                    target *= 0.95f
                    textPaint.textSize = target
                }
            }

            val x = overlay.xPercent * template.widthPx
            val y = overlay.yPercent * template.heightPx
            canvas.drawText(overlay.text, x, y, textPaint)
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
        // Canvas (1200x900) with a 16:9 frame slot (1104x621) to perfectly fit the camera with zero cropping.
        widthPx = 1200,
        heightPx = 900,
        backgroundColor = backgroundColor,
        frames = listOf(FrameSlot(0.04f, 0.04f, 0.92f, 0.69f)), // Perfectly 16:9 (1104x621)
        overlays = buildList {
            add(TextOverlay(title, 0.5f, 0.85f, 24f))
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
