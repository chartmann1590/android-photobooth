package com.example.photobooth.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

interface ImageUploader {
    suspend fun upload(file: File): String
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

        val mimeType = when (file.extension.lowercase()) {
            "gif" -> "image/gif"
            "png" -> "image/png"
            else -> "image/jpeg"
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart(
                "fileToUpload",
                file.name,
                file.asRequestBody(mimeType.toMediaTypeOrNull()),
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
            val url = response.body?.string()?.trim()
            if (url.isNullOrBlank()) {
                throw IllegalStateException("Empty response from host")
            }
            url
        }
    }
}
