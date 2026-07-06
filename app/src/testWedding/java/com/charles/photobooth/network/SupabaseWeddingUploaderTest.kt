package com.charles.photobooth.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SupabaseWeddingUploaderTest {
    @Test
    fun upload_returns_url_from_response() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"id":"abc","url":"https://example.com/photo/abc"}"""))
        server.start()

        val file = File.createTempFile("wedding", ".jpg")
        try {
            val uploader = SupabaseWeddingUploader(
                galleryUrl = server.url("/wedding-gallery").toString().trimEnd('/'),
                uploadToken = "secret",
            )

            val url = runBlocking { uploader.upload(file) }
            val request = server.takeRequest()

            assertEquals("https://example.com/photo/abc", url)
            assertEquals("/wedding-gallery/api/upload", request.path)
            assertEquals("secret", request.getHeader("x-upload-token"))
            assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        } finally {
            file.delete()
            server.shutdown()
        }
    }

    @Test
    fun upload_builds_photo_url_when_only_id_is_returned() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"id":"abc 123"}"""))
        server.start()

        val file = File.createTempFile("wedding", ".jpg")
        try {
            val baseUrl = server.url("/wedding-gallery").toString().trimEnd('/')
            val uploader = SupabaseWeddingUploader(galleryUrl = baseUrl, uploadToken = "secret")

            val url = runBlocking { uploader.upload(file) }

            assertEquals("$baseUrl/photo/abc+123", url)
        } finally {
            file.delete()
            server.shutdown()
        }
    }

    @Test
    fun upload_requires_configuration() {
        val file = File.createTempFile("wedding", ".jpg")
        try {
            val uploader = SupabaseWeddingUploader(galleryUrl = "", uploadToken = "")

            runBlocking { uploader.upload(file) }
            fail("Expected configuration failure")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Wedding gallery URL"))
        } finally {
            file.delete()
        }
    }
}
