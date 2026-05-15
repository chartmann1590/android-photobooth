package com.charles.photobooth.settings

import com.charles.photobooth.template.WatermarkPosition

data class EventSettings(
    val eventName: String = "My Event",
    val eventDate: String = "",
    val filenamePattern: String = "EVENT_yyyyMMdd_HHmmss",
    val currentTemplateId: Long? = null,
    val selectedFrameId: Long? = null,
)

data class CameraSettings(
    val useFrontCamera: Boolean = true,
    val cameraId: String? = null,
)

data class WatermarkSettings(
    val imagePath: String = "",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val sizePercent: Float = 0.15f,
    val enabled: Boolean = false,
)

data class CaptureModeSettings(
    val boothMode: Boolean = false,
    val boothIntervalSeconds: Int = 5,
    val boothPhotoCount: Int = 4,
    val gifModeEnabled: Boolean = false,
    val selectedFilter: String = "NONE",
    val selectedTemplate: String = "NONE",
)

data class UploadSettings(
    val autoUploadEnabled: Boolean = false,
    val useAnonymousHost: Boolean = true,
    val immichBaseUrl: String = "",
    val immichApiToken: String = "",
    val immichAlbumSyncEnabled: Boolean = false,
    val immichAlbumId: String = "",
) {
    val isImmichConfigured: Boolean
        get() = immichBaseUrl.isNotBlank() && immichApiToken.isNotBlank()

    val isAnyUploadDestinationReady: Boolean
        get() = useAnonymousHost || isImmichConfigured
}

data class ShareSettings(
    val enableEmailShare: Boolean = true,
    val enableSmsShare: Boolean = true,
    val enablePrintShare: Boolean = true,
)

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
    val watermark: WatermarkSettings = WatermarkSettings(),
    val captureMode: CaptureModeSettings = CaptureModeSettings(),
    val share: ShareSettings = ShareSettings(),
)

