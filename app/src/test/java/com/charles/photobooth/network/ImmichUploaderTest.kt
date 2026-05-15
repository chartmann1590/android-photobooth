package com.charles.photobooth.network

import com.charles.photobooth.settings.UploadSettings
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = com.charles.photobooth.TestPhotoboothApp::class)
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
                    .scheme("http").host(mockUrl.host).port(mockUrl.port)
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }
            .build()
        return ImmichUploader(settings, client)
    }

    @Test
    fun upload_throws_for_missing_file() {
        val missing = File("/nonexistent/path.jpg")
        try {
            runBlocking { makeUploader().upload(missing) }
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("File not found"))
        }
    }

    @Test
    fun upload_throws_when_base_URL_is_blank() {
        try {
            runBlocking {
                makeUploader(UploadSettings(immichBaseUrl = "", immichApiToken = "token"))
                    .upload(testFile)
            }
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("base URL not configured"))
        }
    }

    @Test
    fun sends_API_key_header() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"abc\"}"))
            makeUploader().upload(testFile)
            val request = server.takeRequest()
            assertEquals("test-token", request.getHeader("x-api-key"))
        }
    }

    @Test
    fun successful_upload_returns_asset_ID() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"asset-123\"}"))
            val id = makeUploader().upload(testFile)
            assertEquals("asset-123", id)
        }
    }

    @Test
    fun upload_throws_on_server_error() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("Error"))
            try {
                makeUploader().upload(testFile)
                fail("Expected exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("Immich upload failed"))
            }
        }
    }

    @Test
    fun upload_throws_when_response_has_no_asset_ID() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"status\":\"created\"}"))
            try {
                makeUploader().upload(testFile)
                fail("Expected exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("no asset ID"))
            }
        }
    }

    @Test
    fun sends_album_ID_when_configured() {
        runBlocking {
            val settings = UploadSettings(
                immichBaseUrl = "http://immich.example.com",
                immichApiToken = "token",
                immichAlbumSyncEnabled = true,
                immichAlbumId = "album-123",
            )
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"abc\"}"))
            makeUploader(settings).upload(testFile)
            val body = server.takeRequest().body.readString(Charsets.UTF_8)
            assertTrue(body.contains("albumId"))
        }
    }

    @Test
    fun no_album_ID_when_album_sync_disabled() {
        runBlocking {
            val settings = UploadSettings(
                immichBaseUrl = "http://immich.example.com",
                immichApiToken = "token",
                immichAlbumSyncEnabled = false,
                immichAlbumId = "album-123",
            )
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"abc\"}"))
            makeUploader(settings).upload(testFile)
            val body = server.takeRequest().body.readString(Charsets.UTF_8)
            assertFalse(body.contains("albumId"))
        }
    }

    @Test
    fun no_album_ID_when_not_configured() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"id\":\"abc\"}"))
            makeUploader().upload(testFile)
            val body = server.takeRequest().body.readString(Charsets.UTF_8)
            assertFalse(body.contains("albumId"))
        }
    }
}
