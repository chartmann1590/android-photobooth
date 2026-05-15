package com.charles.photobooth.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QrCodeGeneratorTest {

    @Test
    fun `generates bitmap of requested size`() {
        val bitmap = QrCodeGenerator.generate("https://example.com", 256)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun `generates bitmap with small size`() {
        val bitmap = QrCodeGenerator.generate("Hello", 64)
        assertEquals(64, bitmap.width)
        assertEquals(64, bitmap.height)
    }

    @Test
    fun `generates bitmap with large size`() {
        val bitmap = QrCodeGenerator.generate("https://example.com/photo/123", 512)
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
    }

    @Test
    fun `generates non-null bitmap`() {
        val bitmap = QrCodeGenerator.generate("test content", 128)
        assertNotNull(bitmap)
    }

    @Test
    fun `different content produces non-identical bitmaps`() {
        val b1 = QrCodeGenerator.generate("Content A", 128)
        val b2 = QrCodeGenerator.generate("Content B", 128)
        assertFalse(b1.sameAs(b2))
    }
}
