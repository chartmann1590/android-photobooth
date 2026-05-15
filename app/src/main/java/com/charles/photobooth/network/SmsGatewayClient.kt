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
