package com.example.photobooth.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photobooth.ui.theme.CardSurface
import com.example.photobooth.ui.theme.DarkBackground
import com.example.photobooth.ui.theme.Gold
import com.example.photobooth.ui.theme.Rose
import com.example.photobooth.ui.theme.RoseLight
import com.example.photobooth.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onStartCapture: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgShift",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        // Animated gradient orbs in background
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .padding(top = (animOffset * 20).dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Rose.copy(alpha = 0.3f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = (animOffset * 15).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Gold.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Main content — landscape layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: branding
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Rose, RoseLight),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PHOTOBOOTH",
                    style = MaterialTheme.typography.displaySmall.copy(
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Capture the moment",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            // Right: action buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ElevatedButton(
                    onClick = onStartCapture,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Rose,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 8.dp,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Start Photobooth",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                FilledTonalButton(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurface,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "View Gallery",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurface,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_preferences),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
