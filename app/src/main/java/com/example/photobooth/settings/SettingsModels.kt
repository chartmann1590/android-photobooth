package com.example.photobooth.settings

data class EventSettings(
    val eventName: String = "My Event",
    val filenamePattern: String = "EVENT_yyyyMMdd_HHmmss",
    val currentTemplateId: Long? = null,
    val selectedFrameId: Long? = null,
)

data class CameraSettings(
    val useFrontCamera: Boolean = true,
)

data class UploadSettings(
    val useAnonymousHost: Boolean = true,
    val anonymousHostType: AnonymousHostType = AnonymousHostType.None,
    val immichBaseUrl: String = "",
    val immichApiToken: String = "",
    val immichAlbumId: String = "",
)

enum class AnonymousHostType {
    None,
    ImgurLike, // placeholder for a concrete provider
}

data class SmsGatewaySettings(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val useCloudServer: Boolean = false,
)

data class SmtpSettings(
    val host: String = "",
    val port: Int = 587,
    val useSslTls: Boolean = true,
    val username: String = "",
    val password: String = "",
    val fromAddress: String = "",
    val fromName: String = "",
    val defaultSubjectTemplate: String = "Your photo from {eventName}",
    val defaultBodyTemplate: String = "Thanks for visiting {eventName}!",
)

data class AllSettings(
    val event: EventSettings = EventSettings(),
    val upload: UploadSettings = UploadSettings(),
    val sms: SmsGatewaySettings = SmsGatewaySettings(),
    val smtp: SmtpSettings = SmtpSettings(),
    val camera: CameraSettings = CameraSettings(),
)

