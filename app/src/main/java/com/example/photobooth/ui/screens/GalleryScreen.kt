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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.photobooth.data.PhotoEntity
import com.example.photobooth.gallery.GalleryViewModel
import com.example.photobooth.ui.theme.CardSurface
import com.example.photobooth.ui.theme.CardSurfaceLight
import com.example.photobooth.ui.theme.DarkBackground
import com.example.photobooth.ui.theme.Gold
import com.example.photobooth.ui.theme.Rose
import com.example.photobooth.ui.theme.TextSecondary

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm: GalleryViewModel = viewModel()
    val photos by vm.photos.collectAsState()
    var selected by remember { mutableStateOf<PhotoEntity?>(null) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Rose,
        unfocusedBorderColor = CardSurfaceLight,
        focusedLabelColor = Rose,
        unfocusedLabelColor = TextSecondary,
        cursorColor = Rose,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Top bar
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
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${photos.size} photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            // Photo grid
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No photos yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                        )
                        Text(
                            text = "Start capturing to see them here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(photos, key = { it.id }) { photo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = photo },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CardSurface,
                            ),
                            elevation = CardDefaults.cardElevation(6.dp),
                        ) {
                            Box {
                                AsyncImage(
                                    model = photo.localPath,
                                    contentDescription = "Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = photo.eventName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (photo.uploadedUrl != null) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                                        contentDescription = "Uploaded",
                                        tint = Gold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Photo detail overlay
        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            selected?.let { photo ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { selected = null },
                    contentAlignment = Alignment.Center,
                ) {
                    // Landscape: image on left, actions on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CardSurface)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {}
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Photo preview
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                        ) {
                            AsyncImage(
                                model = photo.localPath,
                                contentDescription = "Selected photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Actions panel
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = photo.eventName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // SMS section
                            Text(
                                text = "Share via SMS",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                            )
                            FilledTonalButton(
                                onClick = { vm.sendPhotoBySms(photo, phone) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CardSurfaceLight,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_email),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send SMS")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Email section
                            Text(
                                text = "Share via Email",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                            )
                            FilledTonalButton(
                                onClick = { vm.sendPhotoByEmail(photo, email) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CardSurfaceLight,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_email),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Email")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Upload & Print row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ElevatedButton(
                                    onClick = { vm.uploadPhoto(photo) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = Rose,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_upload),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload")
                                }
                                ElevatedButton(
                                    onClick = {
                                        val helper = androidx.print.PrintHelper(context)
                                        helper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
                                        android.graphics.BitmapFactory.decodeFile(photo.localPath)?.let { bitmap ->
                                            helper.printBitmap("Photobooth Photo", bitmap)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = Gold,
                                        contentColor = Color.Black,
                                    ),
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_send),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Print")
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Close button
                            FilledTonalButton(
                                onClick = { selected = null },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CardSurfaceLight.copy(alpha = 0.5f),
                                    contentColor = TextSecondary,
                                ),
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}
