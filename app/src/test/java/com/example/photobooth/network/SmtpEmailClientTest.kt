package com.example.photobooth.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.photobooth.settings.SmtpSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SmtpEmailClientTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `testConnection succeeds for localhost`() = runBlocking {
        val settings = SmtpSettings(host = "localhost")
        val client = SmtpEmailClient(context, settings)
        val result = client.testConnection()
        assertTrue(result.success)
        assertTrue(result.message.contains("DNS lookup successful"))
    }

    @Test
    fun `testConnection fails for invalid hostname`() = runBlocking {
        val settings = SmtpSettings(host = "nonexistent-domain.invalid")
        val client = SmtpEmailClient(context, settings)
        val result = client.testConnection()
        assertFalse(result.success)
        assertTrue(result.message.contains("DNS lookup failed"))
    }

    @Test
    fun `sendPhotoEmail throws for non-existent attachment`() = runBlocking {
        val settings = SmtpSettings(host = "localhost")
        val client = SmtpEmailClient(context, settings)
        val missing = File("/nonexistent/photo.jpg")
        try {
            client.sendPhotoEmail("test@example.com", "Subject", "Body", missing)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Attachment file not found"))
        }
    }

    @Test
    fun `test result data class success`() {
        val result = SmtpEmailClient.TestResult(true, "OK")
        assertTrue(result.success)
        assertEquals("OK", result.message)
    }

    @Test
    fun `test result data class failure`() {
        val result = SmtpEmailClient.TestResult(false, "Failed")
        assertFalse(result.success)
        assertEquals("Failed", result.message)
    }
}
