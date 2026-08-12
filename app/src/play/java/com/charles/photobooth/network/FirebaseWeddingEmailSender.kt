package com.charles.photobooth.network

object FirebaseWeddingEmailSender {
    suspend fun sendEmail(
        recipient: String,
        photoUrl: String,
        eventName: String,
    ) {
        // No-op for play flavor
    }
}
