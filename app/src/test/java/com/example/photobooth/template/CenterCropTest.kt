package com.example.photobooth.template

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CenterCropTest {

    @Test
    fun `wider source crops sides`() {
        val source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val result = centerCropToFill(source, 100, 100)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `taller source crops top and bottom`() {
        val source = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val result = centerCropToFill(source, 100, 100)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `same aspect ratio returns target dimensions`() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = centerCropToFill(source, 50, 50)
        assertEquals(50, result.width)
        assertEquals(50, result.height)
    }

    @Test
    fun `4x3 source to 4x6 target`() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val result = centerCropToFill(source, OUTPUT_4X6_WIDTH, OUTPUT_4X6_HEIGHT)
        assertEquals(OUTPUT_4X6_WIDTH, result.width)
        assertEquals(OUTPUT_4X6_HEIGHT, result.height)
    }

    @Test
    fun `exact match returns target dimensions`() {
        val source = Bitmap.createBitmap(1200, 1800, Bitmap.Config.ARGB_8888)
        val result = centerCropToFill(source, 1200, 1800)
        assertEquals(1200, result.width)
        assertEquals(1800, result.height)
    }

    @Test
    fun `small source upscaled to target`() {
        val source = Bitmap.createBitmap(10, 15, Bitmap.Config.ARGB_8888)
        val result = centerCropToFill(source, 100, 100)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }
}
