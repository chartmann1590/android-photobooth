package com.charles.photobooth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.photobooth.template.OUTPUT_4X6_HEIGHT
import com.charles.photobooth.template.OUTPUT_4X6_WIDTH
import com.charles.photobooth.template.TemplateDefinition

/**
 * Renders a scaled-down preview of a [TemplateDefinition] using the same percentage-based
 * frame and overlay positions the renderer uses at capture time. Frames are shown as
 * gray placeholder rectangles; overlays render as actual text.
 *
 * If [template] is null, a simple full-bleed placeholder is shown to represent
 * "no template" (single photo, no overlay).
 */
@Composable
fun TemplatePreview(
    template: TemplateDefinition?,
    modifier: Modifier = Modifier,
) {
    val aspect = OUTPUT_4X6_WIDTH.toFloat() / OUTPUT_4X6_HEIGHT.toFloat()

    if (template == null) {
        Box(
            modifier = modifier
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333A47))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Photo",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(template.backgroundColor))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
    ) {
        val w = maxWidth
        val h = maxHeight

        template.frames.forEach { frame ->
            Box(
                modifier = Modifier
                    .offset(x = w * frame.leftPercent, y = h * frame.topPercent)
                    .size(width = w * frame.widthPercent, height = h * frame.heightPercent)
                    .background(Color(0xFFB8BEC8).copy(alpha = 0.9f)),
            )
        }

        // Overlays in the real renderer are drawn with baseline ~ y = yPercent * height.
        // Approximate that here: place a full-width centered Text whose top sits a bit
        // above yPercent so the visual center aligns with the renderer's baseline.
        template.overlays.forEach { overlay ->
            // Font size scales with preview height; 18sp full-canvas → ~10% of preview h.
            val fontSp = (h.value * 0.085f * (overlay.textSizeSp / 14f)).coerceIn(7f, 18f)
            val verticalOffset = h * overlay.yPercent - h * 0.07f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = verticalOffset),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = overlay.text.ifBlank { " " },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSp.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
