package com.example.photobooth.network

import com.example.photobooth.settings.SmsGatewaySettings
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
    fun successful_send_completes_without_error() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        makeClient().sendSms(listOf("+1234567890"), "Test message")
    }

    @Test
    fun throws_when_phone_list_is_empty() {
        val exception = try {
            runBlocking {
                val client = SmsGatewayClient(SmsGatewaySettings(baseUrl = "http://example.com"))
                client.sendSms(emptyList(), "Test")
            }
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("No phone numbers"))
    }

    @Test
    fun throws_when_base_URL_is_blank() {
        val exception = try {
            runBlocking {
                val client = SmsGatewayClient(SmsGatewaySettings(baseUrl = ""))
                client.sendSms(listOf("+1234567890"), "Test")
            }
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("base URL not configured"))
    }

    @Test
    fun throws_when_URL_does_not_start_with_http() {
        val exception = try {
            runBlocking {
                val client = SmsGatewayClient(SmsGatewaySettings(baseUrl = "ftp://example.com"))
                client.sendSms(listOf("+1234567890"), "Test")
            }
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("must start with https://"))
    }

    @Test
    fun throws_on_server_error() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server error"))
        val exception = try {
            makeClient().sendSms(listOf("+1234567890"), "Test message")
            null
        } catch (e: IllegalStateException) {
            e
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("SMS send failed"))
    }

    @Test
    fun sends_authorization_header_when_username_is_set() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        makeClient().sendSms(listOf("+1234567890"), "Test")
        val request = server.takeRequest()
        assertNotNull(request.getHeader("Authorization"))
        assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun no_auth_header_when_username_is_blank() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        makeClient(SmsGatewaySettings(baseUrl = "http://example.com", username = ""))
            .sendSms(listOf("+1234567890"), "Test")
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun sends_JSON_body_with_phone_numbers_and_message() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        makeClient().sendSms(listOf("+1234567890", "+0987654321"), "Hello world")
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("+1234567890"))
        assertTrue(body.contains("+0987654321"))
        assertTrue(body.contains("Hello world"))
    }
}
