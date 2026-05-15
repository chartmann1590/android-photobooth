package com.charles.photobooth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF333A47))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Photo",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(template.backgroundColor))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
    ) {
        val w = maxWidth
        val h = maxHeight

        template.frames.forEach { frame ->
            Box(
                modifier = Modifier
                    .offset(x = w * frame.leftPercent, y = h * frame.topPercent)
                    .size(width = w * frame.widthPercent, height = h * frame.heightPercent)
                    .background(Color(0xFFB8BEC8).copy(alpha = 0.85f)),
            )
        }

        template.overlays.forEach { overlay ->
            // Render overlays at a much smaller font scaled to thumbnail size.
            // Approximate: textSizeSp at full canvas → scale to preview height.
            val scaledFontSp = (overlay.textSizeSp * (h.value / OUTPUT_4X6_HEIGHT.toFloat()) * 18f)
                .coerceAtLeast(5f)
            Text(
                text = overlay.text.ifBlank { " " },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = scaledFontSp.sp,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .offset(
                        x = (w * overlay.xPercent) - (w * 0.18f),
                        y = (h * overlay.yPercent) - (h * 0.02f),
                    ),
            )
        }
    }
}
