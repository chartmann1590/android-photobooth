package com.example.photobooth.template

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WatermarkConfigTest {

    @Test
    fun `default values`() {
        val config = WatermarkConfig()
        assertEquals("", config.imagePath)
        assertEquals(WatermarkPosition.BOTTOM_RIGHT, config.position)
        assertEquals(0.15f, config.sizePercent)
        assertEquals(180, config.opacity)
    }

    @Test
    fun `all WatermarkPosition values exist`() {
        assertEquals(6, WatermarkPosition.values().size)
        assertNotNull(WatermarkPosition.valueOf("BOTTOM_RIGHT"))
        assertNotNull(WatermarkPosition.valueOf("BOTTOM_LEFT"))
        assertNotNull(WatermarkPosition.valueOf("BOTTOM_CENTER"))
        assertNotNull(WatermarkPosition.valueOf("TOP_RIGHT"))
        assertNotNull(WatermarkPosition.valueOf("TOP_LEFT"))
        assertNotNull(WatermarkPosition.valueOf("TOP_CENTER"))
    }

    @Test
    fun `copy preserves unchanged values`() {
        val original = WatermarkConfig(imagePath = "/logo.png", opacity = 200)
        val modified = original.copy(position = WatermarkPosition.TOP_LEFT)
        assertEquals("/logo.png", modified.imagePath)
        assertEquals(WatermarkPosition.TOP_LEFT, modified.position)
        assertEquals(200, modified.opacity)
    }

    @Test
    fun `applyWatermark returns original when image path is blank`() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val config = WatermarkConfig(imagePath = "")
        val result = applyWatermark(source, config)
        assertSame(source, result)
    }

    @Test
    fun `applyWatermark returns original when image file does not exist`() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val config = WatermarkConfig(imagePath = "/nonexistent/logo.png")
        val result = applyWatermark(source, config)
        assertSame(source, result)
    }
}
