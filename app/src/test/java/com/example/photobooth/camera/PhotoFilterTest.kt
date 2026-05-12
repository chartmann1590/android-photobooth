package com.example.photobooth.camera

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhotoFilterTest {

    @Test
    fun `NONE filter returns same bitmap`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.NONE)
        assertSame(bitmap, result)
    }

    @Test
    fun `GRAYSCALE produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.GRAYSCALE)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `SEPIA produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.SEPIA)
        assertEquals(80, result.width)
        assertEquals(120, result.height)
    }

    @Test
    fun `BLACK_AND_WHITE produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.BLACK_AND_WHITE)
        assertEquals(50, result.width)
        assertEquals(50, result.height)
    }

    @Test
    fun `VINTAGE produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.VINTAGE)
        assertEquals(200, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun `COOL produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.COOL)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `WARM produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.WARM)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `VIVID produces same dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.VIVID)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `all filters have display names`() {
        for (filter in PhotoFilter.values()) {
            assertTrue(filter.displayName.isNotBlank())
        }
    }

    @Test
    fun `filter enum has expected values`() {
        assertEquals(8, PhotoFilter.values().size)
        assertEquals("Original", PhotoFilter.NONE.displayName)
        assertEquals("Grayscale", PhotoFilter.GRAYSCALE.displayName)
        assertEquals("Sepia", PhotoFilter.SEPIA.displayName)
        assertEquals("B&W", PhotoFilter.BLACK_AND_WHITE.displayName)
        assertEquals("Vintage", PhotoFilter.VINTAGE.displayName)
        assertEquals("Cool", PhotoFilter.COOL.displayName)
        assertEquals("Warm", PhotoFilter.WARM.displayName)
        assertEquals("Vivid", PhotoFilter.VIVID.displayName)
    }

    @Test
    fun `non-NONE filter returns new bitmap instance`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = applyFilter(bitmap, PhotoFilter.GRAYSCALE)
        assertNotSame(bitmap, result)
    }
}
