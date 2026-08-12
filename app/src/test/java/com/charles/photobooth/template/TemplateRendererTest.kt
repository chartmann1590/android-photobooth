package com.charles.photobooth.template

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TemplateRendererTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun solid(color: Int, w: Int = 200, h: Int = 200): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        return bmp
    }

    @Test
    fun `2x2 grid renders to 4x6 canvas`() {
        val template = BuiltInTemplates.photoStrip2x2("Test")
        val bitmaps = listOf(
            solid(Color.RED),
            solid(Color.GREEN),
            solid(Color.BLUE),
            solid(Color.YELLOW),
        )
        val output = TemplateRenderer(context).render(template, bitmaps)
        assertEquals(OUTPUT_4X6_WIDTH, output.width)
        assertEquals(OUTPUT_4X6_HEIGHT, output.height)
    }

    @Test
    fun `vertical strip renders to 4x6 canvas`() {
        val template = BuiltInTemplates.photoStripVertical("Test")
        val bitmaps = listOf(solid(Color.RED), solid(Color.GREEN), solid(Color.BLUE))
        val output = TemplateRenderer(context).render(template, bitmaps)
        assertEquals(OUTPUT_4X6_WIDTH, output.width)
        assertEquals(OUTPUT_4X6_HEIGHT, output.height)
    }

    @Test
    fun `single portrait renders with one bitmap`() {
        val template = BuiltInTemplates.singlePortrait("Test")
        val output = TemplateRenderer(context).render(template, listOf(solid(Color.RED)))
        assertEquals(OUTPUT_4X6_WIDTH, output.width)
        assertEquals(OUTPUT_4X6_HEIGHT, output.height)
    }

    @Test
    fun `renderer tolerates fewer bitmaps than slots`() {
        // A 2x2 template expects 4 bitmaps; passing 2 should not crash —
        // the extra slots are simply skipped.
        val template = BuiltInTemplates.photoStrip2x2("Test")
        val output = TemplateRenderer(context)
            .render(template, listOf(solid(Color.RED), solid(Color.GREEN)))
        assertEquals(OUTPUT_4X6_WIDTH, output.width)
        assertEquals(OUTPUT_4X6_HEIGHT, output.height)
    }

    @Test
    fun `renderer tolerates more bitmaps than slots`() {
        // Single-portrait template has 1 slot; extras must be ignored.
        val template = BuiltInTemplates.singlePortrait("Test")
        val output = TemplateRenderer(context)
            .render(template, List(5) { solid(Color.RED) })
        assertEquals(OUTPUT_4X6_WIDTH, output.width)
        assertEquals(OUTPUT_4X6_HEIGHT, output.height)
    }

    @Test
    fun `2x2 slot math places frames in distinct quadrants`() {
        val template = BuiltInTemplates.photoStrip2x2("Test")
        val centers = template.frames.map { slot ->
            val cx = (slot.leftPercent + slot.widthPercent / 2f) * template.widthPx
            val cy = (slot.topPercent + slot.heightPercent / 2f) * template.heightPx
            cx to cy
        }
        // top-left vs top-right: same y, different x
        assertEquals(centers[0].second, centers[1].second, 1f)
        assertTrue(centers[0].first < centers[1].first)
        // top-left vs bottom-left: same x, different y
        assertEquals(centers[0].first, centers[2].first, 1f)
        assertTrue(centers[0].second < centers[2].second)
        // bottom-right is to the bottom-right of top-left
        assertTrue(centers[3].first > centers[0].first)
        assertTrue(centers[3].second > centers[0].second)
    }

    @Test
    fun `vertical slot math stacks frames top to bottom`() {
        val template = BuiltInTemplates.photoStripVertical("Test")
        val ys = template.frames.map { it.topPercent + it.heightPercent / 2f }
        assertTrue(ys[0] < ys[1])
        assertTrue(ys[1] < ys[2])
    }

    @Test
    fun `output bitmap is mutable ARGB_8888`() {
        val template = BuiltInTemplates.singlePortrait("Test")
        val output = TemplateRenderer(context).render(template, listOf(solid(Color.RED)))
        assertTrue(output.isMutable)
        assertEquals(Bitmap.Config.ARGB_8888, output.config)
    }
}
