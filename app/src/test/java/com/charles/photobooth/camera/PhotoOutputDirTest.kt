package com.charles.photobooth.camera

import android.app.Application
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhotoOutputDirTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `photoOutputDir returns external Pictures directory when available`() {
        val dir = photoOutputDir(app)
        assertNotNull(dir)
        val expected = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        assertEquals(expected, dir)
    }

    @Test
    fun `photoOutputDir creates the directory if missing`() {
        val expected = app.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        // make sure starting state is "directory exists" — Robolectric returns a fresh temp dir
        val dir = photoOutputDir(app)!!
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertEquals(expected.absolutePath, dir.absolutePath)
    }

    @Test
    fun `photoOutputDir path is no longer under filesDir`() {
        // Regression guard: photos used to land in filesDir/photos which is app-private.
        val dir = photoOutputDir(app)!!
        assertTrue(
            "Expected dir under getExternalFilesDir, got ${dir.absolutePath}",
            !dir.absolutePath.startsWith(app.filesDir.absolutePath),
        )
    }
}
