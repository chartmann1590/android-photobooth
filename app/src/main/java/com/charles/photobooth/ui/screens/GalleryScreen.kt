package com.charles.photobooth.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.print.PrintHelper
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.charles.photobooth.R
import com.charles.photobooth.gallery.GalleryActionState
import com.charles.photobooth.gallery.GalleryViewModel
import com.charles.photobooth.util.QrCodeGenerator
import com.charles.photobooth.ui.theme.CardSurface
import com.charles.photobooth.ui.theme.CardSurfaceLight
import com.charles.photobooth.ui.theme.DarkBackground
import com.charles.photobooth.ui.theme.Gold
import com.charles.photobooth.ui.theme.Rose
import com.charles.photobooth.ui.theme.TextSecondary

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm: GalleryViewModel = viewModel()
    val photos by vm.photos.collectAsState()
    val actionState by vm.actionState.collectAsState()
    val shareSettings by vm.shareSettings.collectAsState()
    var selectedPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = remember(photos, selectedPhotoId) {
        selectedPhotoId?.let { id -> photos.firstOrNull { it.id == id } }
    }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var showDeleteConfirmPhotoId by rememberSaveable { mutableStateOf<Long?>(null) }
    val showDeleteConfirm = remember(photos, showDeleteConfirmPhotoId) {
        showDeleteConfirmPhotoId?.let { id -> photos.firstOrNull { it.id == id } }
    }
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                    text = stringResource(R.string.gallery_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.gallery_photo_count, photos.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

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
                            text = stringResource(R.string.gallery_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                        )
                        Text(
                            text = stringResource(R.string.gallery_empty_subtitle),
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
                                .clickable { selectedPhotoId = photo.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CardSurface,
                            ),
                            elevation = CardDefaults.cardElevation(6.dp),
                        ) {
                            Box {
                                AsyncImage(
                                    model = photo.localPath,
                                    imageLoader = imageLoader,
                                    contentDescription = stringResource(R.string.gallery_photo),
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
                                        contentDescription = stringResource(R.string.gallery_uploaded),
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

        selected?.let { photo ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            selectedPhotoId = null
                            vm.clearActionState()
                        },
                    contentAlignment = Alignment.Center,
                ) {
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
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                        ) {
                            AsyncImage(
                                model = photo.localPath,
                                imageLoader = imageLoader,
                                contentDescription = stringResource(R.string.gallery_selected_photo),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

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
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            when (actionState) {
                                is GalleryActionState.Uploading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Rose,
                                            strokeWidth = 3.dp,
                                        )
                                        Text(
                                            text = stringResource(R.string.gallery_uploading),
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                is GalleryActionState.Sending -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Rose,
                                            strokeWidth = 3.dp,
                                        )
                                        Text(
                                            text = stringResource(R.string.gallery_sending),
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                is GalleryActionState.Error -> {
                                    Text(
                                        text = (actionState as GalleryActionState.Error).message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                else -> {}
                            }

                            photo.uploadedUrl?.let { uploadedUrl ->
                                val qrBitmap = remember(uploadedUrl) {
                                    QrCodeGenerator.generate(uploadedUrl, 360)
                                }
                                Text(
                                    text = stringResource(R.string.gallery_qr_code),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                )
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = stringResource(R.string.gallery_qr_code),
                                            modifier = Modifier.size(156.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = uploadedUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (shareSettings.enableSmsShare) {
                                Text(
                                    text = stringResource(R.string.gallery_share_sms),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text(stringResource(R.string.gallery_phone_number)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true,
                                )
                                FilledTonalButton(
                                    onClick = { vm.sendPhotoBySms(photo, phone) },
                                    enabled = phone.isNotBlank() && actionState !is GalleryActionState.Uploading && actionState !is GalleryActionState.Sending,
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
                                    Text(stringResource(R.string.gallery_send_sms))
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            if (shareSettings.enableEmailShare) {
                                Text(
                                    text = stringResource(R.string.gallery_share_email),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary,
                                )
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(stringResource(R.string.gallery_email_address)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors,
                                    singleLine = true,
                                )
                                FilledTonalButton(
                                    onClick = { vm.sendPhotoByEmail(photo, email) },
                                    enabled = email.isNotBlank() && actionState !is GalleryActionState.Uploading && actionState !is GalleryActionState.Sending,
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
                                    Text(stringResource(R.string.gallery_send_email))
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ElevatedButton(
                                    onClick = { vm.uploadPhoto(photo) },
                                    enabled = actionState !is GalleryActionState.Uploading && actionState !is GalleryActionState.Sending,
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
                                    Text(stringResource(R.string.gallery_upload))
                                }
                                if (shareSettings.enablePrintShare) {
                                    ElevatedButton(
                                        onClick = {
                                            val helper = PrintHelper(context)
                                            helper.scaleMode = PrintHelper.SCALE_MODE_FIT
                                            BitmapFactory.decodeFile(photo.localPath)?.let { bitmap ->
                                                helper.printBitmap(context.getString(R.string.print_job_name), bitmap)
                                            }
                                        },
                                        enabled = actionState !is GalleryActionState.Uploading && actionState !is GalleryActionState.Sending,
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
                                        Text(stringResource(R.string.gallery_print))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            FilledTonalButton(
                                onClick = {
                                    vm.getShareIntent(photo)?.let { shareIntent ->
                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.gallery_share_via)))
                                    }
                                },
                                enabled = actionState !is GalleryActionState.Uploading && actionState !is GalleryActionState.Sending,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CardSurfaceLight,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_share),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.gallery_share_social))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            FilledTonalButton(
                                onClick = { showDeleteConfirmPhotoId = photo.id },
                                enabled = actionState !is GalleryActionState.Uploading && actionState !is GalleryActionState.Sending,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.frame_designer_delete))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            FilledTonalButton(
                                onClick = { selectedPhotoId = null; vm.clearActionState() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CardSurfaceLight.copy(alpha = 0.5f),
                                    contentColor = TextSecondary,
                                ),
                            ) {
                                Text(stringResource(R.string.gallery_close))
                            }
                        }
                    }
                }
            }
        }
    }

    showDeleteConfirm?.let { photo ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmPhotoId = null },
            title = { Text(stringResource(R.string.frame_designer_delete_confirm_title)) },
            text = { Text(stringResource(R.string.frame_designer_delete_confirm_message, photo.eventName)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePhoto(photo)
                    showDeleteConfirmPhotoId = null
                    if (selectedPhotoId == photo.id) selectedPhotoId = null
                }) {
                    Text(stringResource(R.string.frame_designer_delete), color = Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmPhotoId = null }) {
                    Text(stringResource(R.string.gallery_close))
                }
            },
        )
    }
}
