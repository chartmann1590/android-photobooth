package com.example.photobooth.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "photobooth_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val EVENT_NAME = stringPreferencesKey("event_name")
        val FILENAME_PATTERN = stringPreferencesKey("filename_pattern")
        val CURRENT_TEMPLATE_ID = stringPreferencesKey("current_template_id")

        val UPLOAD_USE_ANON = stringPreferencesKey("upload_use_anon")
        val UPLOAD_ANON_TYPE = stringPreferencesKey("upload_anon_type")
        val IMMICH_BASE_URL = stringPreferencesKey("immich_base_url")
        val IMMICH_API_TOKEN = stringPreferencesKey("immich_api_token")
        val IMMICH_ALBUM_ID = stringPreferencesKey("immich_album_id")

        val SMS_BASE_URL = stringPreferencesKey("sms_base_url")
        val SMS_USERNAME = stringPreferencesKey("sms_username")
        val SMS_PASSWORD = stringPreferencesKey("sms_password")
        val SMS_USE_CLOUD = stringPreferencesKey("sms_use_cloud")

        val SMTP_HOST = stringPreferencesKey("smtp_host")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SMTP_USE_SSL = stringPreferencesKey("smtp_use_ssl")
        val SMTP_USERNAME = stringPreferencesKey("smtp_username")
        val SMTP_PASSWORD = stringPreferencesKey("smtp_password")
        val SMTP_FROM_ADDRESS = stringPreferencesKey("smtp_from_address")
        val SMTP_FROM_NAME = stringPreferencesKey("smtp_from_name")
        val SMTP_SUBJECT_TEMPLATE = stringPreferencesKey("smtp_subject_template")
        val SMTP_BODY_TEMPLATE = stringPreferencesKey("smtp_body_template")

        val SELECTED_FRAME_ID = stringPreferencesKey("selected_frame_id")
        val USE_FRONT_CAMERA = booleanPreferencesKey("use_front_camera")
    }

    val settingsFlow: Flow<AllSettings> = context.dataStore.data.map { prefs ->
        prefs.toAllSettings()
    }

    suspend fun getCurrentSettings(): AllSettings =
        context.dataStore.data.map { it.toAllSettings() }.first()

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
            prefs[Keys.UPLOAD_ANON_TYPE] = updated.anonymousHostType.name
            prefs[Keys.IMMICH_BASE_URL] = updated.immichBaseUrl
            prefs[Keys.IMMICH_API_TOKEN] = updated.immichApiToken
            prefs[Keys.IMMICH_ALBUM_ID] = updated.immichAlbumId
        }
    }

    suspend fun updateSmsSettings(block: (SmsGatewaySettings) -> SmsGatewaySettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toAllSettings().sms
            val updated = block(current)
            prefs[Keys.SMS_BASE_URL] = updated.baseUrl
            prefs[Keys.SMS_USERNAME] = updated.username
            prefs[Keys.SMS_PASSWORD] = updated.password
            prefs[Keys.SMS_USE_CLOUD] = updated.useCloudServer.toString()
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
            prefs[Keys.SMTP_PASSWORD] = updated.password
            prefs[Keys.SMTP_FROM_ADDRESS] = updated.fromAddress
            prefs[Keys.SMTP_FROM_NAME] = updated.fromName
            prefs[Keys.SMTP_SUBJECT_TEMPLATE] = updated.defaultSubjectTemplate
            prefs[Keys.SMTP_BODY_TEMPLATE] = updated.defaultBodyTemplate
        }
    }

    private fun Preferences.toAllSettings(): AllSettings {
        val event = EventSettings(
            eventName = this[Keys.EVENT_NAME] ?: EventSettings().eventName,
            filenamePattern = this[Keys.FILENAME_PATTERN] ?: EventSettings().filenamePattern,
            currentTemplateId = this[Keys.CURRENT_TEMPLATE_ID]?.toLongOrNull(),
        )

        val upload = UploadSettings(
            useAnonymousHost = (this[Keys.UPLOAD_USE_ANON] ?: "true").toBooleanStrictOrNull() ?: true,
            anonymousHostType = this[Keys.UPLOAD_ANON_TYPE]?.let {
                runCatching { AnonymousHostType.valueOf(it) }.getOrDefault(AnonymousHostType.None)
            } ?: AnonymousHostType.None,
            immichBaseUrl = this[Keys.IMMICH_BASE_URL] ?: "",
            immichApiToken = this[Keys.IMMICH_API_TOKEN] ?: "",
            immichAlbumId = this[Keys.IMMICH_ALBUM_ID] ?: "",
        )

        val sms = SmsGatewaySettings(
            baseUrl = this[Keys.SMS_BASE_URL] ?: "",
            username = this[Keys.SMS_USERNAME] ?: "",
            password = this[Keys.SMS_PASSWORD] ?: "",
            useCloudServer = (this[Keys.SMS_USE_CLOUD] ?: "false").toBooleanStrictOrNull() ?: false,
        )

        val smtp = SmtpSettings(
            host = this[Keys.SMTP_HOST] ?: "",
            port = this[Keys.SMTP_PORT] ?: SmtpSettings().port,
            useSslTls = (this[Keys.SMTP_USE_SSL] ?: "true").toBooleanStrictOrNull() ?: true,
            username = this[Keys.SMTP_USERNAME] ?: "",
            password = this[Keys.SMTP_PASSWORD] ?: "",
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
        )
    }
}

