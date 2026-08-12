package com.charles.photobooth.network

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = com.charles.photobooth.TestPhotoboothApp::class)
class GitHubServiceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun makeService(): GitHubService {
        val mockUrl = server.url("")
        val okClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                // Redirect standard github.com api calls to our local MockWebServer
                val newUrl = original.url.newBuilder()
                    .scheme("http").host(mockUrl.host).port(mockUrl.port)
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }
            .build()
        return GitHubService(okClient)
    }

    @Test
    fun createIssue_sends_correct_payload_and_parses_response() {
        val service = makeService()
        val mockResponseJson = """
            {
                "number": 42,
                "title": "Bug Report",
                "state": "open",
                "created_at": "2026-06-08T00:00:00Z",
                "html_url": "https://github.com/chartmann1590/android-photobooth/issues/42",
                "body": "This is a bug description"
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(201).setBody(mockResponseJson))

        val result = service.createIssue("Bug Report", "This is a bug description")
        assertTrue(result.isSuccess)
        val issue = result.getOrNull()
        assertNotNull(issue)
        assertEquals(42, issue!!.number)
        assertEquals("Bug Report", issue.title)
        assertEquals("open", issue.state)
        assertEquals("https://github.com/chartmann1590/android-photobooth/issues/42", issue.htmlUrl)
        assertEquals("This is a bug description", issue.body)

        val recordedRequest = server.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.endsWith("/issues"))
        val requestBody = recordedRequest.body.readString(Charsets.UTF_8)
        assertTrue(requestBody.contains("Bug Report"))
        assertTrue(requestBody.contains("This is a bug description"))
    }

    @Test
    fun getIssue_parses_response() {
        val service = makeService()
        val mockResponseJson = """
            {
                "number": 42,
                "title": "Bug Report",
                "state": "closed",
                "created_at": "2026-06-08T00:00:00Z",
                "html_url": "https://github.com/chartmann1590/android-photobooth/issues/42",
                "body": "Closed description"
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockResponseJson))

        val result = service.getIssue(42)
        assertTrue(result.isSuccess)
        val issue = result.getOrNull()
        assertNotNull(issue)
        assertEquals(42, issue!!.number)
        assertEquals("closed", issue.state)
        assertEquals("Closed description", issue.body)

        val recordedRequest = server.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.endsWith("/issues/42"))
    }

    @Test
    fun getComments_parses_list() {
        val service = makeService()
        val mockResponseJson = """
            [
                {
                    "id": 101,
                    "body": "First comment",
                    "created_at": "2026-06-08T00:01:00Z",
                    "user": {
                        "login": "someuser"
                    }
                },
                {
                    "id": 102,
                    "body": "Second comment",
                    "created_at": "2026-06-08T00:02:00Z",
                    "user": {
                        "login": "anotheruser"
                    }
                }
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(mockResponseJson))

        val result = service.getComments(42)
        assertTrue(result.isSuccess)
        val comments = result.getOrNull()
        assertNotNull(comments)
        assertEquals(2, comments!!.size)
        assertEquals("First comment", comments[0].body)
        assertEquals("someuser", comments[0].user.login)
        assertEquals("anotheruser", comments[1].user.login)

        val recordedRequest = server.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.endsWith("/issues/42/comments"))
    }

    @Test
    fun postComment_sends_and_parses() {
        val service = makeService()
        val mockResponseJson = """
            {
                "id": 103,
                "body": "New reply",
                "created_at": "2026-06-08T00:03:00Z",
                "user": {
                    "login": "appuser"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(201).setBody(mockResponseJson))

        val result = service.postComment(42, "New reply")
        assertTrue(result.isSuccess)
        val comment = result.getOrNull()
        assertNotNull(comment)
        assertEquals(103L, comment!!.id)
        assertEquals("New reply", comment.body)

        val recordedRequest = server.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.endsWith("/issues/42/comments"))
        val requestBody = recordedRequest.body.readString(Charsets.UTF_8)
        assertTrue(requestBody.contains("New reply"))
    }

    @Test
    fun uploadAsset_sends_base64_and_returns_download_url() {
        val service = makeService()
        val mockResponseJson = """
            {
                "content": {
                    "download_url": "https://raw.githubusercontent.com/chartmann1590/android-photobooth/main/feedback-assets/screenshot.png"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(201).setBody(mockResponseJson))

        val result = service.uploadAsset("screenshot.png", "iVBORw0KGgoAAA...")
        assertTrue(result.isSuccess)
        assertEquals(
            "https://raw.githubusercontent.com/chartmann1590/android-photobooth/main/feedback-assets/screenshot.png",
            result.getOrNull()
        )

        val recordedRequest = server.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertTrue(recordedRequest.path!!.endsWith("/contents/feedback-assets/screenshot.png"))
        val requestBody = recordedRequest.body.readString(Charsets.UTF_8)
        assertTrue(requestBody.contains("iVBORw0KGgoAAA..."))
    }
}
