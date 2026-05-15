package com.charles.photobooth.network

import com.charles.photobooth.settings.SmsGatewaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmsGatewayClient(
    private val settings: SmsGatewaySettings,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    /**
     * Probe the gateway with a HEAD-style GET — confirms reachability and credentials
     * without queueing an SMS. Most gateways (android-sms-gateway included) return 2xx
     * on the base URL when the basic-auth credentials are valid.
     */
    suspend fun testConnection(): NetworkTestResult = withContext(Dispatchers.IO) {
        if (settings.baseUrl.isBlank()) {
            return@withContext NetworkTestResult(false, "Set SMS gateway URL first")
        }
        val base = settings.baseUrl.trimEnd('/')
        if (!base.startsWith("https://") && !base.startsWith("http://")) {
            return@withContext NetworkTestResult(false, "Gateway URL must start with http:// or https://")
        }

        val builder = Request.Builder().url(base).get()
        if (settings.username.isNotBlank()) {
            builder.header("Authorization", Credentials.basic(settings.username, settings.password))
        }

        return@withContext try {
            client.newCall(builder.build()).execute().use { response ->
                when (response.code) {
                    in 200..299 -> NetworkTestResult(true, "Reached gateway (HTTP ${response.code})")
                    401, 403 -> NetworkTestResult(false, "Authentication failed — check username/password")
                    404 -> NetworkTestResult(false, "Endpoint not found — check the Base URL")
                    else -> {
                        val detail = response.body?.string()?.take(200).orEmpty()
                        NetworkTestResult(false, "Gateway returned HTTP ${response.code}${if (detail.isNotBlank()) " — $detail" else ""}")
                    }
                }
            }
        } catch (e: Exception) {
            NetworkTestResult(false, "Could not reach gateway: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    suspend fun sendSms(phones: List<String>, text: String) = withContext(Dispatchers.IO) {
        if (phones.isEmpty()) {
            throw IllegalStateException("No phone numbers provided")
        }
        if (settings.baseUrl.isBlank()) {
            throw IllegalStateException("SMS base URL not configured")
        }

        val base = settings.baseUrl.trimEnd('/')
        if (!base.startsWith("https://") && !base.startsWith("http://")) {
            throw IllegalStateException("SMS base URL must start with https://")
        }

        val json = JSONObject().apply {
            put("textMessage", JSONObject().apply { put("text", text) })
            put("phoneNumbers", phones)
        }
        val body = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val requestBuilder = Request.Builder()
            .url(base)
            .post(body)
            .header("Content-Type", "application/json")

        if (settings.username.isNotBlank()) {
            val cred = Credentials.basic(settings.username, settings.password)
            requestBuilder.header("Authorization", cred)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(200) ?: "No error details"
                throw IllegalStateException("SMS send failed: ${response.code} - $errorBody")
            }
        }
    }
}
