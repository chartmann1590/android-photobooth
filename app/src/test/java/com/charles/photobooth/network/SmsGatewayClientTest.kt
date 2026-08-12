package com.charles.photobooth.network

import com.charles.photobooth.settings.SmsGatewaySettings
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = com.charles.photobooth.TestPhotoboothApp::class)
class SmsGatewayClientTest {
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

    private fun makeClient(settings: SmsGatewaySettings = SmsGatewaySettings(
        baseUrl = "http://sms-gateway.example.com",
        username = "testuser",
        password = "testpass",
    )): SmsGatewayClient {
        val mockUrl = server.url("")
        val okClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val newUrl = original.url.newBuilder()
                    .scheme("http").host(mockUrl.host).port(mockUrl.port)
                    .build()
                chain.proceed(original.newBuilder().url(newUrl).build())
            }
            .build()
        return SmsGatewayClient(settings, okClient)
    }

    @Test
    fun throws_when_phone_list_is_empty() {
        try {
            runBlocking {
                SmsGatewayClient(SmsGatewaySettings(baseUrl = "http://example.com"))
                    .sendSms(emptyList(), "Test")
            }
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("No phone numbers"))
        }
    }

    @Test
    fun throws_when_base_URL_is_blank() {
        try {
            runBlocking {
                SmsGatewayClient(SmsGatewaySettings(baseUrl = ""))
                    .sendSms(listOf("+1234567890"), "Test")
            }
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("base URL not configured"))
        }
    }

    @Test
    fun throws_when_URL_does_not_start_with_http() {
        try {
            runBlocking {
                SmsGatewayClient(SmsGatewaySettings(baseUrl = "ftp://example.com"))
                    .sendSms(listOf("+1234567890"), "Test")
            }
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("must start with https://"))
        }
    }

    @Test
    fun successful_send_completes_without_error() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200))
            makeClient().sendSms(listOf("+1234567890"), "Test message")
        }
    }

    @Test
    fun throws_on_server_error() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("Server error"))
            try {
                makeClient().sendSms(listOf("+1234567890"), "Test message")
                fail("Expected exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("SMS send failed"))
            }
        }
    }

    @Test
    fun sends_authorization_header_when_username_is_set() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200))
            makeClient().sendSms(listOf("+1234567890"), "Test")
            val request = server.takeRequest()
            assertNotNull(request.getHeader("Authorization"))
            assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
        }
    }

    @Test
    fun no_auth_header_when_username_is_blank() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200))
            makeClient(SmsGatewaySettings(baseUrl = "http://example.com", username = ""))
                .sendSms(listOf("+1234567890"), "Test")
            val request = server.takeRequest()
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test
    fun sends_JSON_body_with_phone_numbers_and_message() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200))
            makeClient().sendSms(listOf("+1234567890", "+0987654321"), "Hello world")
            val body = server.takeRequest().body.readString(Charsets.UTF_8)
            assertTrue(body.contains("+1234567890"))
            assertTrue(body.contains("+0987654321"))
            assertTrue(body.contains("Hello world"))
        }
    }

    @Test
    fun testConnection_returns_failure_when_base_URL_blank() = runBlocking {
        val result = SmsGatewayClient(SmsGatewaySettings(baseUrl = "")).testConnection()
        assertFalse(result.success)
        assertTrue(result.message.contains("URL", ignoreCase = true))
    }

    @Test
    fun testConnection_returns_failure_when_URL_scheme_invalid() = runBlocking {
        val result = SmsGatewayClient(SmsGatewaySettings(baseUrl = "ftp://example.com"))
            .testConnection()
        assertFalse(result.success)
        assertTrue(result.message.contains("http", ignoreCase = true))
    }

    @Test
    fun testConnection_returns_success_on_2xx_and_does_not_send_SMS() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(204))
            val result = makeClient().testConnection()
            assertTrue("expected success, got: ${result.message}", result.success)
            assertTrue(result.message.contains("204") || result.message.contains("gateway", ignoreCase = true))
            // Verify the probe used GET, not POST — i.e., no SMS was queued.
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals(0, request.bodySize)
        }
    }

    @Test
    fun testConnection_reports_auth_failure_on_401() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(401))
            val result = makeClient().testConnection()
            assertFalse(result.success)
            assertTrue(result.message.contains("Authentication", ignoreCase = true))
        }
    }

    @Test
    fun testConnection_reports_auth_failure_on_403() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(403))
            val result = makeClient().testConnection()
            assertFalse(result.success)
            assertTrue(result.message.contains("Authentication", ignoreCase = true))
        }
    }

    @Test
    fun testConnection_reports_url_error_on_404() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(404))
            val result = makeClient().testConnection()
            assertFalse(result.success)
            assertTrue(result.message.contains("not found", ignoreCase = true))
        }
    }

    @Test
    fun testConnection_includes_response_body_on_5xx() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("backend offline"))
            val result = makeClient().testConnection()
            assertFalse(result.success)
            assertTrue(result.message.contains("500"))
            assertTrue(result.message.contains("backend offline"))
        }
    }

    @Test
    fun testConnection_sends_basic_auth_header_when_username_set() {
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200))
            makeClient().testConnection()
            val request = server.takeRequest()
            val auth = request.getHeader("Authorization")
            assertNotNull(auth)
            assertTrue(auth!!.startsWith("Basic "))
        }
    }
}
