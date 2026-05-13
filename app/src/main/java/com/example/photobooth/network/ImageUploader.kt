package com.example.photobooth.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

interface ImageUploader {
    suspend fun upload(file: File): String
}

private fun mimeTypeFor(file: File): String = when (file.extension.lowercase()) {
    "gif" -> "image/gif"
    "png" -> "image/png"
    else -> "image/jpeg"
}

private fun requireUploadUrl(value: String?, hostName: String): String {
    val url = value?.trim().orEmpty()
    if (url.isBlank()) {
        throw IllegalStateException("Empty response from $hostName")
    }
    if (!url.startsWith("https://") && !url.startsWith("http://")) {
        throw IllegalStateException("Upload did not return a URL from $hostName: ${url.take(120)}")
    }
    return url
}

class AnonymousUploader(
    private val uploaders: List<ImageUploader> = listOf(
        StorageToUploader(),
        CatboxUploader(),
    ),
) : ImageUploader {
    override suspend fun upload(file: File): String {
        var lastError: Exception? = null
        uploaders.forEach { uploader ->
            try {
                return uploader.upload(file)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException(
            "Anonymous upload failed: ${lastError?.message ?: "no upload hosts configured"}",
            lastError,
        )
    }
}

class StorageToUploader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
) : ImageUploader {
    override suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalStateException("File not found: ${file.name}")
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mimeTypeFor(file).toMediaTypeOrNull()),
            )
            .build()

        val request = Request.Builder()
            .url("https://storage.to/api/sharex/upload")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed: ${response.code} - ${text.take(200)}")
            }
            val json = JSONObject(text)
            if (!json.optBoolean("success", false)) {
                val message = json.optString("message", "host rejected upload")
                throw IllegalStateException("Upload failed: $message")
            }
            requireUploadUrl(json.optString("url", json.optString("raw_url", "")), "storage.to")
        }
    }
}

class CatboxUploader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
) : ImageUploader {
    override suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalStateException("File not found: ${file.name}")
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart(
                "fileToUpload",
                file.name,
                file.asRequestBody(mimeTypeFor(file).toMediaTypeOrNull()),
            )
            .build()

        val request = Request.Builder()
            .url("https://catbox.moe/user/api.php")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed: ${response.code}")
            }
            requireUploadUrl(response.body?.string(), "catbox.moe")
        }
    }
}
