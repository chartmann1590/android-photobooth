package com.example.photobooth.network

import com.example.photobooth.settings.UploadSettings
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ImmichUploaderTest {
    private lateinit var server: MockWebServer
    private lateinit var uploader: ImmichUploader
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
        val settings = UploadSettings(
            immichBaseUrl = "http://immich.example.com",
            immichApiToken = "test-token",
        )
        uploader = ImmichUploader(settings, client)
        testFile = File.createTempFile("test", ".jpg")
        testFile.writeText("fake image data")
    }

    @After
    fun tearDown() {
        server.shutdown()
        testFile.delete()
    }

    @Test
    fun `successful upload returns asset ID`() = runBlocking {
        server.enqueue(MockResponse()
            .setBody("{\"id\":\"asset-123\",\"status\":\"created\"}")
            .setHeader("Content-Type", "application/json"))
        val id = uploader.upload(testFile)
        assertEquals("asset-123", id)
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
    fun `upload throws when base URL is blank`() = runBlocking {
        val settings = UploadSettings(immichBaseUrl = "", immichApiToken = "token")
        val localUploader = ImmichUploader(settings)
        try {
            localUploader.upload(testFile)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("base URL not configured"))
        }
    }

    @Test
    fun `upload throws on server error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        try {
            uploader.upload(testFile)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Immich upload failed"))
        }
    }

    @Test
    fun `upload throws when response has no asset ID`() = runBlocking {
        server.enqueue(MockResponse()
            .setBody("{\"status\":\"created\"}")
            .setHeader("Content-Type", "application/json"))
        try {
            uploader.upload(testFile)
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("no asset ID"))
        }
    }

    @Test
    fun `sends API key header`() = runBlocking {
        server.enqueue(MockResponse()
            .setBody("{\"id\":\"abc\"}")
            .setHeader("Content-Type", "application/json"))
        uploader.upload(testFile)
        val request = server.takeRequest()
        assertEquals("test-token", request.getHeader("x-api-key"))
    }

    @Test
    fun `sends album ID when configured`() = runBlocking {
        val settings = UploadSettings(
            immichBaseUrl = "http://immich.example.com",
            immichApiToken = "token",
            immichAlbumId = "album-123",
        )
        val mockUrl = server.url("")
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val newUrl = original.url.newBuilder()
                    .scheme("http").host(mockUrl.host).port(mockUrl.port)
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }
            .build()
        val localUploader = ImmichUploader(settings, client)
        server.enqueue(MockResponse()
            .setBody("{\"id\":\"abc\"}")
            .setHeader("Content-Type", "application/json"))
        localUploader.upload(testFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("albumId"))
        assertTrue(body.contains("album-123"))
    }

    @Test
    fun `no album ID when not configured`() = runBlocking {
        server.enqueue(MockResponse()
            .setBody("{\"id\":\"abc\"}")
            .setHeader("Content-Type", "application/json"))
        uploader.upload(testFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertFalse(body.contains("albumId"))
    }
}
