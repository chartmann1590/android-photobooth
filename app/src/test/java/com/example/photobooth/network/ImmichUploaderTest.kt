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

    private fun makeUploader(settings: UploadSettings = UploadSettings(
        immichBaseUrl = "http://immich.example.com",
        immichApiToken = "test-token",
    )): ImmichUploader {
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
        return ImmichUploader(settings, client)
    }

    private fun jsonSuccessResponse(json: String): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .setHeader("Content-Type", "application/json")
    }

    @Test
    fun successful_upload_returns_asset_ID() = runBlocking {
        server.enqueue(jsonSuccessResponse("{\"id\":\"asset-123\",\"status\":\"created\"}"))
        val id = makeUploader().upload(testFile)
        assertEquals("asset-123", id)
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
    fun upload_throws_when_base_URL_is_blank() {
        val exception = try {
            runBlocking {
                makeUploader(UploadSettings(immichBaseUrl = "", immichApiToken = "token"))
                    .upload(testFile)
            }
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("base URL not configured"))
    }

    @Test
    fun upload_throws_on_server_error() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        val exception = try {
            makeUploader().upload(testFile)
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("Immich upload failed"))
    }

    @Test
    fun upload_throws_when_response_has_no_asset_ID() = runBlocking {
        server.enqueue(jsonSuccessResponse("{\"status\":\"created\"}"))
        val exception = try {
            makeUploader().upload(testFile)
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("no asset ID"))
    }

    @Test
    fun sends_API_key_header() = runBlocking {
        server.enqueue(jsonSuccessResponse("{\"id\":\"abc\"}"))
        makeUploader().upload(testFile)
        val request = server.takeRequest()
        assertEquals("test-token", request.getHeader("x-api-key"))
    }

    @Test
    fun sends_album_ID_when_configured() = runBlocking {
        val settings = UploadSettings(
            immichBaseUrl = "http://immich.example.com",
            immichApiToken = "token",
            immichAlbumId = "album-123",
        )
        server.enqueue(jsonSuccessResponse("{\"id\":\"abc\"}"))
        makeUploader(settings).upload(testFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("albumId"))
        assertTrue(body.contains("album-123"))
    }

    @Test
    fun no_album_ID_when_not_configured() = runBlocking {
        server.enqueue(jsonSuccessResponse("{\"id\":\"abc\"}"))
        makeUploader().upload(testFile)
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertFalse(body.contains("albumId"))
    }
}
