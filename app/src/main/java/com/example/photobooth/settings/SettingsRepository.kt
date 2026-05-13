package com.example.photobooth.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "photobooth_settings")

class SettingsRepository(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "photobooth_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private object Keys {
        val EVENT_NAME = stringPreferencesKey("event_name")
        val FILENAME_PATTERN = stringPreferencesKey("filename_pattern")
        val CURRENT_TEMPLATE_ID = stringPreferencesKey("current_template_id")

        val UPLOAD_USE_ANON = stringPreferencesKey("upload_use_anon")
        val IMMICH_BASE_URL = stringPreferencesKey("immich_base_url")
        val IMMICH_ALBUM_SYNC_ENABLED = stringPreferencesKey("immich_album_sync_enabled")
        val IMMICH_ALBUM_ID = stringPreferencesKey("immich_album_id")

        val SMS_BASE_URL = stringPreferencesKey("sms_base_url")
        val SMS_USERNAME = stringPreferencesKey("sms_username")
        val SMS_USE_CLOUD = stringPreferencesKey("sms_use_cloud")

        val SMTP_HOST = stringPreferencesKey("smtp_host")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SMTP_USE_SSL = stringPreferencesKey("smtp_use_ssl")
        val SMTP_USERNAME = stringPreferencesKey("smtp_username")
        val SMTP_FROM_ADDRESS = stringPreferencesKey("smtp_from_address")
        val SMTP_FROM_NAME = stringPreferencesKey("smtp_from_name")
        val SMTP_SUBJECT_TEMPLATE = stringPreferencesKey("smtp_subject_template")
        val SMTP_BODY_TEMPLATE = stringPreferencesKey("smtp_body_template")

        val SELECTED_FRAME_ID = stringPreferencesKey("selected_frame_id")
        val USE_FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
        val CAMERA_ID = stringPreferencesKey("camera_id")

        val WATERMARK_ENABLED = stringPreferencesKey("watermark_enabled")
        val WATERMARK_IMAGE_PATH = stringPreferencesKey("watermark_image_path")
        val WATERMARK_POSITION = stringPreferencesKey("watermark_position")
        val WATERMARK_SIZE_PERCENT = stringPreferencesKey("watermark_size_percent")

        val BOOTH_MODE = stringPreferencesKey("booth_mode")
        val BOOTH_INTERVAL = stringPreferencesKey("booth_interval")
        val BOOTH_PHOTO_COUNT = stringPreferencesKey("booth_photo_count")
        val GIF_MODE_ENABLED = stringPreferencesKey("gif_mode_enabled")
        val SELECTED_FILTER = stringPreferencesKey("selected_filter")
        val SELECTED_TEMPLATE = stringPreferencesKey("selected_template")
    }

    private object SecureKeys {
        const val IMMICH_API_TOKEN = "immich_api_token"
        const val SMS_PASSWORD = "sms_password"
        const val SMTP_PASSWORD = "smtp_password"
    }

    val settingsFlow: Flow<AllSettings> = context.dataStore.data.map { prefs ->
        prefs.toAllSettings()
    }

    suspend fun getCurrentSettings(): AllSettings =
        context.dataStore.data.map { it.toAllSettings() }.first()

    fun getCurrentSettingsBlocking(): AllSettings {
        val prefs = runCatching {
            kotlinx.coroutines.runBlocking {
                context.dataStore.data.first()
            }
        }.getOrNull()
        return prefs?.toAllSettings() ?: AllSettings()
    }

    private fun getSecureString(key: String): String {
        return encryptedPrefs.getString(key, "") ?: ""
    }

    private fun setSecureString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    suspend fun updateEventSettings(block: (EventSettings) -> EventSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().event
            val updated = block(current)
            prefs[Keys.EVENT_NAME] = updated.eventName
            prefs[Keys.FILENAME_PATTERN] = updated.filenamePattern
            prefs[Keys.CURRENT_TEMPLATE_ID] = updated.currentTemplateId?.toString() ?: ""
            prefs[Keys.SELECTED_FRAME_ID] = updated.selectedFrameId?.toString() ?: ""
        }
    }

    suspend fun updateCameraSettings(block: (CameraSettings) -> CameraSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().camera
            val updated = block(current)
            prefs[Keys.USE_FRONT_CAMERA] = updated.useFrontCamera
            prefs[Keys.CAMERA_ID] = updated.cameraId ?: ""
        }
    }

    suspend fun updateWatermarkSettings(block: (WatermarkSettings) -> WatermarkSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().watermark
            val updated = block(current)
            prefs[Keys.WATERMARK_ENABLED] = updated.enabled.toString()
            prefs[Keys.WATERMARK_IMAGE_PATH] = updated.imagePath
            prefs[Keys.WATERMARK_POSITION] = updated.position.name
            prefs[Keys.WATERMARK_SIZE_PERCENT] = updated.sizePercent.toString()
        }
    }

    suspend fun updateCaptureModeSettings(block: (CaptureModeSettings) -> CaptureModeSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().captureMode
            val updated = block(current)
            prefs[Keys.BOOTH_MODE] = updated.boothMode.toString()
            prefs[Keys.BOOTH_INTERVAL] = updated.boothIntervalSeconds.toString()
            prefs[Keys.BOOTH_PHOTO_COUNT] = updated.boothPhotoCount.toString()
            prefs[Keys.GIF_MODE_ENABLED] = updated.gifModeEnabled.toString()
            prefs[Keys.SELECTED_FILTER] = updated.selectedFilter
            prefs[Keys.SELECTED_TEMPLATE] = updated.selectedTemplate
        }
    }

    suspend fun updateSelectedFrame(frameId: Long?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_FRAME_ID] = frameId?.toString() ?: ""
        }
    }

    suspend fun updateUploadSettings(block: (UploadSettings) -> UploadSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().upload
            val updated = block(current)
            prefs[Keys.UPLOAD_USE_ANON] = updated.useAnonymousHost.toString()
            prefs[Keys.IMMICH_BASE_URL] = updated.immichBaseUrl
            prefs[Keys.IMMICH_ALBUM_SYNC_ENABLED] = updated.immichAlbumSyncEnabled.toString()
            prefs[Keys.IMMICH_ALBUM_ID] = updated.immichAlbumId
            setSecureString(SecureKeys.IMMICH_API_TOKEN, updated.immichApiToken)
        }
    }

    suspend fun updateSmsSettings(block: (SmsGatewaySettings) -> SmsGatewaySettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().sms
            val updated = block(current)
            prefs[Keys.SMS_BASE_URL] = updated.baseUrl
            prefs[Keys.SMS_USERNAME] = updated.username
            prefs[Keys.SMS_USE_CLOUD] = updated.useCloudServer.toString()
            setSecureString(SecureKeys.SMS_PASSWORD, updated.password)
        }
    }

    suspend fun updateSmtpSettings(block: (SmtpSettings) -> SmtpSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().smtp
            val updated = block(current)
            prefs[Keys.SMTP_HOST] = updated.host
            prefs[Keys.SMTP_PORT] = updated.port
            prefs[Keys.SMTP_USE_SSL] = updated.useSslTls.toString()
            prefs[Keys.SMTP_USERNAME] = updated.username
            prefs[Keys.SMTP_FROM_ADDRESS] = updated.fromAddress
            prefs[Keys.SMTP_FROM_NAME] = updated.fromName
            prefs[Keys.SMTP_SUBJECT_TEMPLATE] = updated.defaultSubjectTemplate
            prefs[Keys.SMTP_BODY_TEMPLATE] = updated.defaultBodyTemplate
            setSecureString(SecureKeys.SMTP_PASSWORD, updated.password)
        }
    }

    private fun Preferences.toAllSettings(): AllSettings {
        val event = EventSettings(
            eventName = this[Keys.EVENT_NAME] ?: EventSettings().eventName,
            filenamePattern = this[Keys.FILENAME_PATTERN] ?: EventSettings().filenamePattern,
            currentTemplateId = this[Keys.CURRENT_TEMPLATE_ID]?.toLongOrNull(),
            selectedFrameId = this[Keys.SELECTED_FRAME_ID]?.toLongOrNull(),
        )

        val camera = CameraSettings(
            useFrontCamera = this[Keys.USE_FRONT_CAMERA] ?: true,
            cameraId = this[Keys.CAMERA_ID]?.ifBlank { null },
        )

        val watermark = WatermarkSettings(
            enabled = (this[Keys.WATERMARK_ENABLED] ?: "false").toBooleanStrictOrNull() ?: false,
            imagePath = this[Keys.WATERMARK_IMAGE_PATH] ?: "",
            position = this[Keys.WATERMARK_POSITION]?.let {
                runCatching { com.example.photobooth.template.WatermarkPosition.valueOf(it) }
                    .getOrDefault(com.example.photobooth.template.WatermarkPosition.BOTTOM_RIGHT)
            } ?: com.example.photobooth.template.WatermarkPosition.BOTTOM_RIGHT,
            sizePercent = this[Keys.WATERMARK_SIZE_PERCENT]?.toFloatOrNull() ?: 0.15f,
        )

        val captureMode = CaptureModeSettings(
            boothMode = (this[Keys.BOOTH_MODE] ?: "false").toBooleanStrictOrNull() ?: false,
            boothIntervalSeconds = this[Keys.BOOTH_INTERVAL]?.toIntOrNull() ?: 5,
            boothPhotoCount = this[Keys.BOOTH_PHOTO_COUNT]?.toIntOrNull() ?: 4,
            gifModeEnabled = (this[Keys.GIF_MODE_ENABLED] ?: "false").toBooleanStrictOrNull() ?: false,
            selectedFilter = this[Keys.SELECTED_FILTER] ?: "NONE",
            selectedTemplate = this[Keys.SELECTED_TEMPLATE] ?: "NONE",
        )

        val upload = UploadSettings(
            useAnonymousHost = (this[Keys.UPLOAD_USE_ANON] ?: "true").toBooleanStrictOrNull() ?: true,
            immichBaseUrl = this[Keys.IMMICH_BASE_URL] ?: "",
            immichApiToken = getSecureString(SecureKeys.IMMICH_API_TOKEN),
            immichAlbumSyncEnabled = (this[Keys.IMMICH_ALBUM_SYNC_ENABLED] ?: "false").toBooleanStrictOrNull() ?: false,
            immichAlbumId = this[Keys.IMMICH_ALBUM_ID] ?: "",
        )

        val sms = SmsGatewaySettings(
            baseUrl = this[Keys.SMS_BASE_URL] ?: "",
            username = this[Keys.SMS_USERNAME] ?: "",
            password = getSecureString(SecureKeys.SMS_PASSWORD),
            useCloudServer = (this[Keys.SMS_USE_CLOUD] ?: "false").toBooleanStrictOrNull() ?: false,
        )

        val smtp = SmtpSettings(
            host = this[Keys.SMTP_HOST] ?: "",
            port = this[Keys.SMTP_PORT] ?: SmtpSettings().port,
            useSslTls = (this[Keys.SMTP_USE_SSL] ?: "true").toBooleanStrictOrNull() ?: true,
            username = this[Keys.SMTP_USERNAME] ?: "",
            password = getSecureString(SecureKeys.SMTP_PASSWORD),
            fromAddress = this[Keys.SMTP_FROM_ADDRESS] ?: "",
            fromName = this[Keys.SMTP_FROM_NAME] ?: "",
            defaultSubjectTemplate = this[Keys.SMTP_SUBJECT_TEMPLATE] ?: SmtpSettings().defaultSubjectTemplate,
            defaultBodyTemplate = this[Keys.SMTP_BODY_TEMPLATE] ?: SmtpSettings().defaultBodyTemplate,
        )

        return AllSettings(
            event = event,
            upload = upload,
            sms = sms,
            smtp = smtp,
            camera = camera,
            watermark = watermark,
            captureMode = captureMode,
        )
    }
}
