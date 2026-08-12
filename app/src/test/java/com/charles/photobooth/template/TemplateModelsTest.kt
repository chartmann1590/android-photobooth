package com.charles.photobooth.template

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateConstantsTest {

    @Test
    fun `output dimensions are 4x6 ratio at 300 DPI`() {
        assertEquals(1200, OUTPUT_4X6_WIDTH)
        assertEquals(1800, OUTPUT_4X6_HEIGHT)
    }

    @Test
    fun `frame slot data class`() {
        val slot = FrameSlot(
            leftPercent = 0.1f,
            topPercent = 0.2f,
            widthPercent = 0.5f,
            heightPercent = 0.6f,
        )
        assertEquals(0.1f, slot.leftPercent)
        assertEquals(0.2f, slot.topPercent)
        assertEquals(0.5f, slot.widthPercent)
        assertEquals(0.6f, slot.heightPercent)
    }

    @Test
    fun `template definition data class`() {
        val def = TemplateDefinition(
            widthPx = 1200,
            heightPx = 1800,
            frames = listOf(
                FrameSlot(0.1f, 0.1f, 0.8f, 0.8f),
            ),
            overlays = listOf(
                TextOverlay("Hello", 0.5f, 0.95f, 16f),
            ),
        )
        assertEquals(1200, def.widthPx)
        assertEquals(1800, def.heightPx)
        assertEquals(1, def.frames.size)
        assertEquals(1, def.overlays.size)
        assertEquals("Hello", def.overlays[0].text)
    }

    @Test
    fun `text overlay data class`() {
        val overlay = TextOverlay("Event Name", 0.5f, 0.9f, 24f)
        assertEquals("Event Name", overlay.text)
        assertEquals(0.5f, overlay.xPercent)
        assertEquals(0.9f, overlay.yPercent)
        assertEquals(24f, overlay.textSizeSp)
    }
}
