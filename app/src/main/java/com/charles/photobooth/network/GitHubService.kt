package com.charles.photobooth.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class GitHubIssue(
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("html_url") val htmlUrl: String,
    val body: String? = null
)

@Serializable
data class GitHubComment(
    val id: Long,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    val user: GitHubUser
)

@Serializable
data class GitHubUser(
    val login: String
)

@Serializable
private data class GitHubIssueRequest(
    val title: String,
    val body: String
)

@Serializable
private data class GitHubCommentRequest(
    val body: String
)

@Serializable
private data class GitHubUploadRequest(
    val filename: String,
    val contentBase64: String
)

@Serializable
private data class GitHubUploadResponse(
    val content: GitHubContentDetail
)

@Serializable
private data class GitHubContentDetail(
    @SerialName("download_url") val downloadUrl: String
)

/**
 * Talks to the cloudflare-worker/ feedback relay, not api.github.com directly — the
 * Worker holds the GitHub token as a server-side secret and hardcodes this app's own
 * repo, so no owner/repo/credential ever needs to travel through this app. Previously
 * embedded BuildConfig.GITHUB_API_TOKEN client-side as a Bearer header, which shipped a
 * real repo-write PAT in every release build (extractable from the APK). See
 * cloudflare-worker/src/index.ts.
 */
class GitHubService(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val apiBaseUrl = "https://android-photobooth-github-feedback.charles-h-hartmann1.workers.dev"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun newRequestBuilder(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Android-Photobooth-App")
    }

    fun createIssue(title: String, body: String): Result<GitHubIssue> {
        val url = "$apiBaseUrl/issue"
        val requestBodyJson = json.encodeToString(GitHubIssueRequest.serializer(), GitHubIssueRequest(title, body))
        val request = newRequestBuilder(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to create issue: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                json.decodeFromString(GitHubIssue.serializer(), bodyString)
            }
        }
    }

    fun getIssue(number: Int): Result<GitHubIssue> {
        val url = "$apiBaseUrl/issue/$number"
        val request = newRequestBuilder(url)
            .get()
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to get issue: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                json.decodeFromString(GitHubIssue.serializer(), bodyString)
            }
        }
    }

    fun getComments(number: Int): Result<List<GitHubComment>> {
        val url = "$apiBaseUrl/issue/$number/comments"
        val request = newRequestBuilder(url)
            .get()
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to get comments: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(GitHubComment.serializer()), bodyString)
            }
        }
    }

    fun postComment(number: Int, body: String): Result<GitHubComment> {
        val url = "$apiBaseUrl/issue/$number/comments"
        val requestBodyJson = json.encodeToString(GitHubCommentRequest.serializer(), GitHubCommentRequest(body))
        val request = newRequestBuilder(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to post comment: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                json.decodeFromString(GitHubComment.serializer(), bodyString)
            }
        }
    }

    fun uploadAsset(filename: String, base64Data: String): Result<String> {
        val url = "$apiBaseUrl/upload-image"
        val requestBodyJson = json.encodeToString(
            GitHubUploadRequest.serializer(),
            GitHubUploadRequest(filename = filename, contentBase64 = base64Data),
        )
        val request = newRequestBuilder(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to upload asset: ${response.code} ${response.message} ${response.body?.string()}")
                }
                val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                val uploadResponse = json.decodeFromString(GitHubUploadResponse.serializer(), bodyString)
                uploadResponse.content.downloadUrl
            }
        }
    }
}
