package com.example.photobooth.template

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RenderSimple4x6Test {

    @Test
    fun `without frame overlay returns 4x6 bitmap`() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val result = renderSimple4x6(source, null)
        assertEquals(OUTPUT_4X6_WIDTH, result.width)
        assertEquals(OUTPUT_4X6_HEIGHT, result.height)
    }

    @Test
    fun `with non-existent frame overlay still returns 4x6 bitmap`() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val result = renderSimple4x6(source, "/nonexistent/frame.png")
        assertEquals(OUTPUT_4X6_WIDTH, result.width)
        assertEquals(OUTPUT_4X6_HEIGHT, result.height)
    }

    @Test
    fun `with watermark config but blank path returns bitmap`() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val watermark = WatermarkConfig(imagePath = "")
        val result = renderSimple4x6(source, null, watermark)
        assertEquals(OUTPUT_4X6_WIDTH, result.width)
        assertEquals(OUTPUT_4X6_HEIGHT, result.height)
    }

    @Test
    fun `with null watermark returns bitmap`() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val result = renderSimple4x6(source, null, null)
        assertEquals(OUTPUT_4X6_WIDTH, result.width)
        assertEquals(OUTPUT_4X6_HEIGHT, result.height)
    }

    @Test
    fun `square source upscaled to 4x6`() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = renderSimple4x6(source, null)
        assertEquals(OUTPUT_4X6_WIDTH, result.width)
        assertEquals(OUTPUT_4X6_HEIGHT, result.height)
    }
}
