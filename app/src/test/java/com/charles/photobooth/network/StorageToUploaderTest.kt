package com.charles.photobooth.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = com.charles.photobooth.TestPhotoboothApp::class)
class StorageToUploaderTest {
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

    private fun makeUploader(): StorageToUploader {
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
        return StorageToUploader(client)
    }

    @Test
    fun upload_returns_url_on_success() {
        runBlocking {
            server.enqueue(
                MockResponse().setBody(
                    """{"success":true,"url":"https://storage.to/abc","raw_url":"https://storage.to/r/abc"}""",
                ),
            )
            val url = makeUploader().upload(testFile)
            assertEquals("https://storage.to/abc", url)
        }
    }

    @Test
    fun sends_multipart_upload() {
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"success":true,"url":"https://storage.to/abc"}"""))
            makeUploader().upload(testFile)
            val body = server.takeRequest().body.readString(Charsets.UTF_8)
            assertTrue(body.contains("name=\"file\""))
            assertTrue(body.contains("image/jpeg"))
        }
    }

    @Test
    fun sends_mp4_mime_type() {
        runBlocking {
            val mp4File = File.createTempFile("test", ".mp4")
            mp4File.writeText("fake video data")
            server.enqueue(MockResponse().setBody("""{"success":true,"url":"https://storage.to/abc.mp4"}"""))
            makeUploader().upload(mp4File)
            val body = server.takeRequest().body.readString(Charsets.UTF_8)
            assertTrue(body.contains("video/mp4"))
            mp4File.delete()
        }
    }

    @Test
    fun upload_throws_when_host_rejects_file() {
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"success":false,"message":"No file"}"""))
            try {
                makeUploader().upload(testFile)
                fail("Expected exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("No file"))
            }
        }
    }

    @Test
    fun upload_throws_on_non_url_response() {
        runBlocking {
            server.enqueue(MockResponse().setBody("""{"success":true,"url":"not a url"}"""))
            try {
                makeUploader().upload(testFile)
                fail("Expected exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("did not return a URL"))
            }
        }
    }
}
