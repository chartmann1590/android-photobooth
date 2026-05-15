package com.charles.photobooth.camera

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoCaptureStateTest {

    @Test
    fun `idle state`() {
        val state = VideoCaptureState.Idle
        assertEquals("Idle", state::class.simpleName)
    }

    @Test
    fun `recording state`() {
        val state = VideoCaptureState.Recording
        assertEquals("Recording", state::class.simpleName)
    }

    @Test
    fun `finished state holds uri`() {
        val uri = Uri.parse("content://media/video.mp4")
        val state = VideoCaptureState.Finished(uri)
        assertEquals(uri, state.uri)
    }

    @Test
    fun `error state holds message`() {
        val state = VideoCaptureState.Error("Recording failed")
        assertEquals("Recording failed", state.message)
    }

    @Test
    fun `video duration limit is 8 seconds`() {
        assertEquals(8, MAX_VIDEO_DURATION_SECONDS)
    }
}
