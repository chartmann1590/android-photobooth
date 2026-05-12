package com.example.photobooth.ui

import org.junit.Assert.*
import org.junit.Test

class NavGraphScreenTest {

    @Test
    fun `Home route`() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun `Capture route`() {
        assertEquals("capture", Screen.Capture.route)
    }

    @Test
    fun `Gallery route`() {
        assertEquals("gallery", Screen.Gallery.route)
    }

    @Test
    fun `Settings route`() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun `FrameDesigner route`() {
        assertEquals("frame_designer", Screen.FrameDesigner.route)
    }

    @Test
    fun `all routes are unique`() {
        val routes = Screen::class.sealedSubclasses.mapNotNull {
            it.objectInstance?.route
        }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `screen hierarchy`() {
        val screens = listOf(
            Screen.Home,
            Screen.Capture,
            Screen.Gallery,
            Screen.Settings,
            Screen.FrameDesigner,
        )
        assertEquals(5, screens.size)
        screens.forEach { assertTrue(it is Screen) }
    }
}
