package com.example.photobooth.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class CatboxUploaderTest {
    private lateinit var server: MockWebServer
    private lateinit var uploader: CatboxUploader
    private lateinit var testFile: File

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val mockUrl = server.url("")
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val newUrl = original.url.newBuilder()
                    .scheme("http")
                    .host(mockUrl.host)
                    .port(mockUrl.port)
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }
            .build()
        uploader = CatboxUploader(client)
        testFile = File.createTempFile("test", ".jpg")
        testFile.writeText("fake image data")
    }

    @After
    fun tearDown() {
        server.shutdown()
        testFile.delete()
    }

    @Test
    fun `successful upload returns URL`() = runBlocking {
        server.enqueue(MockResponse().setBody("https://catbox.moe/abc123.jpg"))
        val url = uploader.upload(testFile)
        assertEquals("https://catbox.moe/abc123.jpg", url)
    }

    @Test
    fun `upload throws for non-existent file`() = runBlocking {
        val missing = File("/nonexistent/path.jpg")
        try {
            uploader.upload(missing)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("File not found"))
        }
    }

    @Test
    fun `upload throws on server error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        try {
            uploader.upload(testFile)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Upload failed"))
        }
    }

    @Test
    fun `upload throws on empty response`() = runBlocking {
        server.enqueue(MockResponse().setBody(""))
        try {
            uploader.upload(testFile)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Empty response"))
        }
    }

    @Test
    fun `sends file as multipart upload`() = runBlocking {
        server.enqueue(MockResponse().setBody("https://catbox.moe/abc.jpg"))
        uploader.upload(testFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("fileToUpload"))
        assertTrue(body.contains("reqtype"))
    }

    @Test
    fun `detects jpg mime type`() = runBlocking {
        val jpgFile = File.createTempFile("test", ".jpg")
        jpgFile.writeText("data")
        server.enqueue(MockResponse().setBody("https://example.com/a.jpg"))
        uploader.upload(jpgFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("image/jpeg"))
        jpgFile.delete()
    }

    @Test
    fun `detects png mime type`() = runBlocking {
        val pngFile = File.createTempFile("test", ".png")
        pngFile.writeText("data")
        server.enqueue(MockResponse().setBody("https://example.com/a.png"))
        uploader.upload(pngFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("image/png"))
        pngFile.delete()
    }

    @Test
    fun `detects gif mime type`() = runBlocking {
        val gifFile = File.createTempFile("test", ".gif")
        gifFile.writeText("data")
        server.enqueue(MockResponse().setBody("https://example.com/a.gif"))
        uploader.upload(gifFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("image/gif"))
        gifFile.delete()
    }
}
