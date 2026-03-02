package com.example.photobooth.network

import com.example.photobooth.settings.SmsGatewaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SmsGatewayClient(
    private val settings: SmsGatewaySettings,
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun sendSms(phones: List<String>, text: String) = withContext(Dispatchers.IO) {
        val base = settings.baseUrl.trimEnd('/')
        val url = base
        val json = JSONObject().apply {
            put("textMessage", JSONObject().apply { put("text", text) })
            put("phoneNumbers", phones)
        }
        val body = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")

        if (settings.username.isNotBlank()) {
            val cred = Credentials.basic(settings.username, settings.password)
            requestBuilder.header("Authorization", cred)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("SMS send failed: ${response.code}")
            }
        }
    }
}

