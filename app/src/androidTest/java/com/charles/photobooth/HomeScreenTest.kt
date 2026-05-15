package com.charles.photobooth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.charles.photobooth.ui.screens.HomeScreen
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysTitleAndButtons() {
        composeTestRule.setContent {
            HomeScreen(
                onStartCapture = {},
                onOpenGallery = {},
                onOpenSettings = {},
                onOpenTutorial = {},
                onDonate = {},
            )
        }

        composeTestRule.onNodeWithText("PHOTOBOOTH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Capture the moment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Photobooth").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tutorial").assertIsDisplayed()
        composeTestRule.onNodeWithText("Support Charles on Buy Me a Coffee").assertIsDisplayed()
    }

    @Test
    fun homeScreen_startButtonCallback() {
        var captureClicked = false

        composeTestRule.setContent {
            HomeScreen(
                onStartCapture = { captureClicked = true },
                onOpenGallery = {},
                onOpenSettings = {},
                onOpenTutorial = {},
                onDonate = {},
            )
        }

        composeTestRule.onNodeWithText("Start Photobooth").performClick()
        assert(captureClicked)
    }
}
