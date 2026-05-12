package com.example.photobooth.template

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GifEncoderTest {

    @Test
    fun `writes GIF89a header`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10)
        encoder.start(output)
        encoder.finish()
        val bytes = output.toByteArray()
        assertTrue(bytes.size >= 6)
        assertEquals("GIF89a", String(bytes, 0, 6))
    }

    @Test
    fun `finish writes trailer byte`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10)
        encoder.start(output)
        encoder.addFrame(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        encoder.finish()
        val bytes = output.toByteArray()
        assertEquals(0x3B, bytes.last().toInt())
    }

    @Test
    fun `single frame produces valid output`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10)
        encoder.start(output)
        encoder.addFrame(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        encoder.finish()
        val bytes = output.toByteArray()
        assertTrue(bytes.size > 6)
        assertEquals("GIF89a", String(bytes, 0, 6))
        assertEquals(0x3B, bytes.last().toInt())
    }

    @Test
    fun `multiple frames produce larger output than single frame`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10)
        encoder.start(output)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        encoder.addFrame(bitmap)
        val singleFrameSize = output.size()
        encoder.addFrame(bitmap)
        encoder.finish()
        assertTrue(output.size() > singleFrameSize)
    }

    @Test
    fun `output contains NETSCAPE looping extension`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10, 0)
        encoder.start(output)
        encoder.addFrame(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        encoder.finish()
        val bytes = output.toByteArray()
        val content = String(bytes, Charsets.US_ASCII)
        assertTrue(content.contains("NETSCAPE2.0"))
    }

    @Test
    fun `encoder with custom delay`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10, delayMs = 1000)
        encoder.start(output)
        encoder.addFrame(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        encoder.finish()
        assertTrue(output.size() > 0)
    }

    @Test
    fun `encoder with custom dimensions`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(20, 30)
        encoder.start(output)
        encoder.addFrame(Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888))
        encoder.finish()
        val bytes = output.toByteArray()
        assertTrue(bytes.size > 0)
        assertEquals("GIF89a", String(bytes, 0, 6))
    }

    @Test
    fun `scaled bitmap when dimensions differ`() {
        val output = ByteArrayOutputStream()
        val encoder = GifEncoder(10, 10)
        encoder.start(output)
        encoder.addFrame(Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888))
        encoder.finish()
        val bytes = output.toByteArray()
        assertTrue(bytes.size > 0)
    }
}
