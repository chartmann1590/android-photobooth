package com.example.photobooth.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photobooth.R
import com.example.photobooth.ui.theme.CardSurface
import com.example.photobooth.ui.theme.CardSurfaceLight
import com.example.photobooth.ui.theme.DarkBackground
import com.example.photobooth.ui.theme.Gold
import com.example.photobooth.ui.theme.Rose
import com.example.photobooth.ui.theme.TextSecondary

private data class TutorialStep(
    val titleRes: Int,
    val iconRes: Int,
    val bodyRes: Int,
)

private val tutorialSteps = listOf(
    TutorialStep(
        titleRes = R.string.tutorial_step_welcome_title,
        iconRes = android.R.drawable.ic_menu_camera,
        bodyRes = R.string.tutorial_step_welcome_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_permissions_title,
        iconRes = android.R.drawable.ic_dialog_info,
        bodyRes = R.string.tutorial_step_permissions_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_home_title,
        iconRes = android.R.drawable.ic_menu_manage,
        bodyRes = R.string.tutorial_step_home_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_capture_title,
        iconRes = android.R.drawable.ic_menu_camera,
        bodyRes = R.string.tutorial_step_capture_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_booth_title,
        iconRes = android.R.drawable.ic_menu_crop,
        bodyRes = R.string.tutorial_step_booth_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_templates_title,
        iconRes = android.R.drawable.ic_menu_gallery,
        bodyRes = R.string.tutorial_step_templates_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_gif_title,
        iconRes = android.R.drawable.ic_menu_view,
        bodyRes = R.string.tutorial_step_gif_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_filters_title,
        iconRes = android.R.drawable.ic_menu_edit,
        bodyRes = R.string.tutorial_step_filters_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_frames_title,
        iconRes = android.R.drawable.ic_menu_gallery,
        bodyRes = R.string.tutorial_step_frames_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_watermark_title,
        iconRes = android.R.drawable.ic_menu_edit,
        bodyRes = R.string.tutorial_step_watermark_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_gallery_title,
        iconRes = android.R.drawable.ic_menu_gallery,
        bodyRes = R.string.tutorial_step_gallery_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_upload_title,
        iconRes = android.R.drawable.ic_menu_upload,
        bodyRes = R.string.tutorial_step_upload_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_qr_title,
        iconRes = android.R.drawable.ic_menu_info_details,
        bodyRes = R.string.tutorial_step_qr_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_share_title,
        iconRes = android.R.drawable.ic_menu_share,
        bodyRes = R.string.tutorial_step_share_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_print_title,
        iconRes = android.R.drawable.ic_menu_send,
        bodyRes = R.string.tutorial_step_print_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_settings_title,
        iconRes = android.R.drawable.ic_menu_preferences,
        bodyRes = R.string.tutorial_step_settings_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_privacy_title,
        iconRes = android.R.drawable.ic_lock_lock,
        bodyRes = R.string.tutorial_step_privacy_body,
    ),
    TutorialStep(
        titleRes = R.string.tutorial_step_support_title,
        iconRes = android.R.drawable.ic_menu_send,
        bodyRes = R.string.tutorial_step_support_body,
    ),
)

@Composable
fun TutorialScreen(
    onBack: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[currentStep]
    val isLast = currentStep == tutorialSteps.lastIndex
    val isFirst = currentStep == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardSurfaceLight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = stringResource(R.string.capture_back),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.tutorial_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${currentStep + 1} / ${tutorialSteps.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / tutorialSteps.size },
            modifier = Modifier.fillMaxWidth().height(3.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Rose.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = step.iconRes),
                            contentDescription = null,
                            tint = Rose,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Text(
                        text = stringResource(step.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    val bodyText = stringResource(step.bodyRes)
                    val annotatedBody = buildAnnotatedString {
                        val parts = bodyText.split("**")
                        parts.forEachIndexed { index, part ->
                            if (index % 2 == 0) {
                                append(part)
                            } else {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Gold)) {
                                    append(part)
                                }
                            }
                        }
                    }
                    Text(
                        text = annotatedBody,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(tutorialSteps.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == currentStep) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentStep) Rose
                                else CardSurfaceLight,
                            ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = { if (!isFirst) currentStep-- },
                    enabled = !isFirst,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurfaceLight,
                        contentColor = Color.White,
                        disabledContainerColor = CardSurfaceLight.copy(alpha = 0.3f),
                        disabledContentColor = TextSecondary,
                    ),
                ) {
                    Text(stringResource(R.string.tutorial_back))
                }
                ElevatedButton(
                    onClick = {
                        if (isLast) onBack() else currentStep++
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Rose,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        if (isLast) stringResource(R.string.tutorial_done)
                        else stringResource(R.string.tutorial_next)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
            .background(CardSurfaceLight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Rose)
                .let { mod ->
                    val fraction = progress()
                    mod.fillMaxWidth(fraction)
                },
        )
    }
}
