package com.charles.photobooth.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelsTest {

    @Test
    fun `event settings defaults`() {
        val settings = EventSettings()
        assertEquals("My Event", settings.eventName)
        assertEquals("", settings.eventDate)
        assertEquals("EVENT_yyyyMMdd_HHmmss", settings.filenamePattern)
        assertNull(settings.currentTemplateId)
        assertNull(settings.selectedFrameId)
    }

    @Test
    fun `event settings preserves date through copy`() {
        val original = EventSettings(eventName = "Sarah's 30th", eventDate = "May 24, 2026")
        val modified = original.copy(eventName = "Bob's 40th")
        assertEquals("May 24, 2026", modified.eventDate)
        assertEquals("Bob's 40th", modified.eventName)
    }

    @Test
    fun `upload settings defaults`() {
        val settings = UploadSettings()
        assertFalse(settings.autoUploadEnabled)
        assertTrue(settings.useAnonymousHost)
        assertEquals("", settings.immichBaseUrl)
        assertEquals("", settings.immichApiToken)
        assertFalse(settings.immichAlbumSyncEnabled)
        assertEquals("", settings.immichAlbumId)
        assertFalse(settings.isImmichConfigured)
    }

    @Test
    fun `isAnyUploadDestinationReady true when anonymous host enabled`() {
        val settings = UploadSettings(useAnonymousHost = true)
        assertTrue(settings.isAnyUploadDestinationReady)
    }

    @Test
    fun `isAnyUploadDestinationReady true when Immich configured`() {
        val settings = UploadSettings(
            useAnonymousHost = false,
            immichBaseUrl = "https://immich.example.com",
            immichApiToken = "token",
        )
        assertTrue(settings.isAnyUploadDestinationReady)
    }

    @Test
    fun `isAnyUploadDestinationReady false when neither anonymous nor Immich is set`() {
        val settings = UploadSettings(
            useAnonymousHost = false,
            immichBaseUrl = "",
            immichApiToken = "",
        )
        assertFalse(settings.isAnyUploadDestinationReady)
    }

    @Test
    fun `share settings defaults all enabled`() {
        val settings = ShareSettings()
        assertTrue(settings.enableEmailShare)
        assertTrue(settings.enableSmsShare)
        assertTrue(settings.enablePrintShare)
    }

    @Test
    fun `share settings copy disables individual channels`() {
        val original = ShareSettings()
        val modified = original.copy(enableEmailShare = false, enablePrintShare = false)
        assertFalse(modified.enableEmailShare)
        assertTrue(modified.enableSmsShare)
        assertFalse(modified.enablePrintShare)
    }

    @Test
    fun `sms settings defaults`() {
        val settings = SmsGatewaySettings()
        assertEquals("", settings.baseUrl)
        assertEquals("", settings.username)
        assertEquals("", settings.password)
        assertFalse(settings.useCloudServer)
    }

    @Test
    fun `smtp settings defaults`() {
        val settings = SmtpSettings()
        assertEquals("", settings.host)
        assertEquals(587, settings.port)
        assertTrue(settings.useSslTls)
        assertEquals("", settings.username)
        assertEquals("", settings.password)
        assertEquals("", settings.fromAddress)
        assertEquals("", settings.fromName)
        assertEquals("Your photo from {eventName}", settings.defaultSubjectTemplate)
        assertEquals("Thanks for visiting {eventName}!", settings.defaultBodyTemplate)
    }

    @Test
    fun `camera settings defaults`() {
        val settings = CameraSettings()
        assertTrue(settings.useFrontCamera)
        assertFalse(settings.frontScreenFlashEnabled)
    }

    @Test
    fun `capture mode settings defaults`() {
        val settings = CaptureModeSettings()
        assertFalse(settings.boothMode)
        assertFalse(settings.gifModeEnabled)
        assertEquals(4, settings.boothPhotoCount)
        assertEquals("NONE", settings.selectedFilter)
        assertEquals("NONE", settings.selectedTemplate)
        assertTrue(settings.disabledTemplateKeys.isEmpty())
    }

    @Test
    fun `isTemplateEnabled treats unknown keys as enabled by default`() {
        val settings = CaptureModeSettings()
        assertTrue(settings.isTemplateEnabled("STRIP_2x2"))
        assertTrue(settings.isTemplateEnabled("EVENT_BIRTHDAY"))
        assertTrue(settings.isTemplateEnabled("EVENT_HALLOWEEN_FUTURE"))
    }

    @Test
    fun `isTemplateEnabled is false for keys in the disabled set`() {
        val settings = CaptureModeSettings(disabledTemplateKeys = setOf("EVENT_WEDDING", "STRIP_VERTICAL"))
        assertFalse(settings.isTemplateEnabled("EVENT_WEDDING"))
        assertFalse(settings.isTemplateEnabled("STRIP_VERTICAL"))
        assertTrue(settings.isTemplateEnabled("EVENT_BIRTHDAY"))
        assertTrue(settings.isTemplateEnabled("NONE"))
    }

    @Test
    fun `all settings composes from defaults`() {
        val settings = AllSettings()
        assertEquals("My Event", settings.event.eventName)
        assertTrue(settings.upload.useAnonymousHost)
        assertEquals(587, settings.smtp.port)
        assertTrue(settings.camera.useFrontCamera)
        assertTrue(settings.share.enableEmailShare)
        assertTrue(settings.share.enableSmsShare)
        assertTrue(settings.share.enablePrintShare)
        assertFalse(settings.upload.autoUploadEnabled)
    }

    @Test
    fun `smtp email template placeholder replacement`() {
        val settings = SmtpSettings()
        val subject = settings.defaultSubjectTemplate.replace("{eventName}", "Wedding 2026")
        assertEquals("Your photo from Wedding 2026", subject)

        val body = settings.defaultBodyTemplate.replace("{eventName}", "Wedding 2026")
        assertEquals("Thanks for visiting Wedding 2026!", body)
    }

    @Test
    fun `data class copy preserves values`() {
        val original = UploadSettings(useAnonymousHost = true, immichBaseUrl = "https://photos.example.com")
        val modified = original.copy(useAnonymousHost = false)
        assertFalse(modified.useAnonymousHost)
        assertEquals("https://photos.example.com", modified.immichBaseUrl)
    }

    @Test
    fun `isImmichConfigured is true when both base url and token are set`() {
        val settings = UploadSettings(
            immichBaseUrl = "https://immich.example.com",
            immichApiToken = "abc123",
        )
        assertTrue(settings.isImmichConfigured)
    }

    @Test
    fun `isImmichConfigured is false when missing token`() {
        val settings = UploadSettings(immichBaseUrl = "https://immich.example.com")
        assertFalse(settings.isImmichConfigured)
    }
}
