package com.charles.photobooth.camera

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOrientationTest {

    @Test
    fun `readExifOrientation returns NORMAL for missing file`() {
        val result = readExifOrientation("/does/not/exist/photo.jpg")
        assertEquals(ExifInterface.ORIENTATION_NORMAL, result)
    }

    @Test
    fun `readExifOrientation returns NORMAL for empty path`() {
        val result = readExifOrientation("")
        assertEquals(ExifInterface.ORIENTATION_NORMAL, result)
    }
}
