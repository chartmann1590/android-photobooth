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
    private lateinit var testFile: File

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        testFile = File.createTempFile("test", ".jpg")
        testFile.writeText("fake image data")
    }

    @After
    fun tearDown() {
        server.shutdown()
        testFile.delete()
    }

    private fun makeUploader(): CatboxUploader {
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
        return CatboxUploader(client)
    }

    @Test
    fun upload_returns_url_on_success() = runBlocking {
        server.enqueue(MockResponse().setBody("https://catbox.moe/abc123.jpg"))
        val url = makeUploader().upload(testFile)
        assertEquals("https://catbox.moe/abc123.jpg", url)
    }

    @Test
    fun upload_throws_for_missing_file() {
        val missing = File("/nonexistent/path.jpg")
        val exception = try {
            runBlocking { makeUploader().upload(missing) }
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("File not found"))
    }

    @Test
    fun upload_throws_on_server_error() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val exception = try {
            makeUploader().upload(testFile)
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("Upload failed"))
    }

    @Test
    fun upload_throws_on_empty_response() = runBlocking {
        server.enqueue(MockResponse().setBody(""))
        val exception = try {
            makeUploader().upload(testFile)
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("Empty response"))
    }

    @Test
    fun sends_multipart_upload() = runBlocking {
        server.enqueue(MockResponse().setBody("https://catbox.moe/abc.jpg"))
        makeUploader().upload(testFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("fileToUpload"))
        assertTrue(body.contains("reqtype"))
    }

    @Test
    fun detects_jpg_mime_type() = runBlocking {
        val jpgFile = File.createTempFile("test", ".jpg")
        jpgFile.writeText("data")
        server.enqueue(MockResponse().setBody("https://example.com/a.jpg"))
        makeUploader().upload(jpgFile)
        val body = server.takeRequest().body.readString(Charsets.UTF_8)
        assertTrue(body.contains("image/jpeg"))
        jpgFile.delete()
    }

    @Test
    fun detects_png_mime_type() = runBlocking {
        val pngFile = File.createTempFile("test", ".png")
        pngFile.writeText("data")
        server.enqueue(MockResponse().setBody("https://example.com/a.png"))
        makeUploader().upload(pngFile)
        val body = server.takeRequest().body.readString(Charsets.UTF_8)
        assertTrue(body.contains("image/png"))
        pngFile.delete()
    }

    @Test
    fun detects_gif_mime_type() = runBlocking {
        val gifFile = File.createTempFile("test", ".gif")
        gifFile.writeText("data")
        server.enqueue(MockResponse().setBody("https://example.com/a.gif"))
        makeUploader().upload(gifFile)
        val body = server.takeRequest().body.readString(Charsets.UTF_8)
        assertTrue(body.contains("image/gif"))
        gifFile.delete()
    }
}
