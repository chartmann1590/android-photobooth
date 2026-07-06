package com.charles.photobooth.network

import com.charles.photobooth.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SupabaseWeddingUploader(
    private val galleryUrl: String = BuildConfig.WEDDING_GALLERY_URL,
    private val uploadToken: String = BuildConfig.WEDDING_UPLOAD_TOKEN,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build(),
) : ImageUploader {
    override suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalStateException("File not found: ${file.name}")
        }
        val baseUrl = galleryUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            throw IllegalStateException("Wedding gallery URL is not configured")
        }
        if (uploadToken.isBlank()) {
            throw IllegalStateException("Wedding upload token is not configured")
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("event", "Charles & Jessica Hartmann Wedding")
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mediaMimeTypeFor(file).toMediaTypeOrNull()),
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/upload")
            .header("x-upload-token", uploadToken)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Wedding upload failed: ${response.code} - ${text.take(200)}")
            }
            val url = jsonStringValue(text, "url").ifBlank {
                jsonStringValue(text, "id").takeIf { it.isNotBlank() }?.let { id ->
                    "$baseUrl/photo/${URLEncoder.encode(id, "UTF-8")}"
                }.orEmpty()
            }
            requireWeddingUploadUrl(url)
        }
    }

    private fun jsonStringValue(text: String, name: String): String =
        runCatching {
            Json.parseToJsonElement(text).jsonObject[name]?.jsonPrimitive?.content.orEmpty()
        }.getOrDefault("")

    private fun requireWeddingUploadUrl(value: String): String {
        val url = value.trim()
        if (url.isBlank()) {
            throw IllegalStateException("Empty response from wedding gallery")
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw IllegalStateException("Wedding upload did not return a URL: ${url.take(120)}")
        }
        return url
    }
}
