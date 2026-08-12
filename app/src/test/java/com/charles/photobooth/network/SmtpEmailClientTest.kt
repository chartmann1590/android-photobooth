package com.charles.photobooth.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.charles.photobooth.settings.SmtpSettings
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
    fun `testConnection returns failure for blank host`() = runBlocking {
        val client = SmtpEmailClient(context, SmtpSettings(host = ""))
        val result = client.testConnection()
        assertFalse(result.success)
        assertTrue(result.message.contains("host", ignoreCase = true))
    }

    @Test
    fun `testConnection returns failure for unreachable host`() = runBlocking {
        // Pick a TLD that DNS will refuse and a low timeout: we want the test to fail
        // fast and deterministically without hitting the network on CI.
        val settings = SmtpSettings(
            host = "nonexistent-domain.invalid",
            port = 587,
            username = "user",
            password = "pass",
        )
        val result = SmtpEmailClient(context, settings).testConnection()
        assertFalse(result.success)
        // Any of these wordings is acceptable — we just care that it failed loudly.
        val msg = result.message.lowercase()
        assertTrue(
            "expected reachability or DNS-style error message, got: ${result.message}",
            msg.contains("reach") || msg.contains("host") || msg.contains("unknown")
                || msg.contains("nonexistent-domain"),
        )
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
    fun `NetworkTestResult success holds message`() {
        val result = NetworkTestResult(true, "OK")
        assertTrue(result.success)
        assertEquals("OK", result.message)
    }

    @Test
    fun `NetworkTestResult failure holds message`() {
        val result = NetworkTestResult(false, "Failed")
        assertFalse(result.success)
        assertEquals("Failed", result.message)
    }
}
