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
    private lateinit var client: SmsGatewayClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
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
        val settings = SmsGatewaySettings(
            baseUrl = "http://sms-gateway.example.com",
            username = "testuser",
            password = "testpass",
        )
        client = SmsGatewayClient(settings, okClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful send completes without error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        client.sendSms(listOf("+1234567890"), "Test message")
    }

    @Test
    fun `throws when phone list is empty`() = runBlocking {
        val settings = SmsGatewaySettings(baseUrl = "http://example.com")
        val smsClient = SmsGatewayClient(settings)
        try {
            smsClient.sendSms(emptyList(), "Test")
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("No phone numbers"))
        }
    }

    @Test
    fun `throws when base URL is blank`() = runBlocking {
        val settings = SmsGatewaySettings(baseUrl = "")
        val smsClient = SmsGatewayClient(settings)
        try {
            smsClient.sendSms(listOf("+1234567890"), "Test")
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("base URL not configured"))
        }
    }

    @Test
    fun `throws when URL does not start with http`() = runBlocking {
        val settings = SmsGatewaySettings(baseUrl = "ftp://example.com")
        val smsClient = SmsGatewayClient(settings)
        try {
            smsClient.sendSms(listOf("+1234567890"), "Test")
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("must start with https://"))
        }
    }

    @Test
    fun `throws on server error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Server error"))
        try {
            client.sendSms(listOf("+1234567890"), "Test message")
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("SMS send failed"))
        }
    }

    @Test
    fun `sends authorization header when username is set`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        client.sendSms(listOf("+1234567890"), "Test")
        val request = server.takeRequest()
        assertNotNull(request.getHeader("Authorization"))
        assertTrue(request.getHeader("Authorization")!!.startsWith("Basic "))
    }

    @Test
    fun `no auth header when username is blank`() = runBlocking {
        val settings = SmsGatewaySettings(baseUrl = "http://example.com", username = "")
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
        val noAuthClient = SmsGatewayClient(settings, okClient)
        server.enqueue(MockResponse().setResponseCode(200))
        noAuthClient.sendSms(listOf("+1234567890"), "Test")
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `sends JSON body with phone numbers and message`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        client.sendSms(listOf("+1234567890", "+0987654321"), "Hello world")
        val request = server.takeRequest()
        val body = request.body.readString(Charsets.UTF_8)
        assertTrue(body.contains("+1234567890"))
        assertTrue(body.contains("+0987654321"))
        assertTrue(body.contains("Hello world"))
    }
}
