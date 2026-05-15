package com.charles.photobooth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.charles.photobooth.ui.screens.ConsentDialog
import org.junit.Rule
import org.junit.Test

class ConsentDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun consentDialog_displaysTitleAndButtons() {
        var decisionMade = false

        composeTestRule.setContent {
            ConsentDialog(onDecision = { decisionMade = true })
        }

        composeTestRule.onNodeWithText("Help us improve").assertIsDisplayed()
        composeTestRule.onNodeWithText("Allow").assertIsDisplayed()
        composeTestRule.onNodeWithText("No thanks").assertIsDisplayed()
    }

    @Test
    fun consentDialog_acceptButtonCallsCallback() {
        var result: Boolean? = null

        composeTestRule.setContent {
            ConsentDialog(onDecision = { result = true })
        }

        composeTestRule.onNodeWithText("Allow").performClick()
        assert(result != null)
    }

    @Test
    fun consentDialog_declineButtonCallsCallback() {
        var result: Boolean? = null

        composeTestRule.setContent {
            ConsentDialog(onDecision = { result = false })
        }

        composeTestRule.onNodeWithText("No thanks").performClick()
        assert(result != null)
    }
}
