package com.charles.photobooth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.photobooth.R
import com.charles.photobooth.monetization.BillingUiState
import com.charles.photobooth.monetization.MAX_FREE_AND_AD_PHOTOS_PER_DAY
import com.charles.photobooth.monetization.PHOTOS_PER_REWARDED_AD
import com.charles.photobooth.monetization.PhotoQuotaState
import com.charles.photobooth.monetization.RewardedAdState
import com.charles.photobooth.ui.theme.Gold
import com.charles.photobooth.ui.theme.Rose

@Composable
fun PaywallDialog(
    quotaState: PhotoQuotaState,
    billingState: BillingUiState,
    rewardedAdState: RewardedAdState,
    onWatchAd: () -> Unit,
    onBuyUnlimited: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.paywall_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val remainingText = if (quotaState.hasUnlimitedPhotos) {
                    stringResource(R.string.paywall_unlimited_owned)
                } else {
                    stringResource(R.string.paywall_remaining, quotaState.remainingPhotos)
                }
                Text(remainingText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.paywall_body, MAX_FREE_AND_AD_PHOTOS_PER_DAY))
                if (!quotaState.canEarnAdReward && !quotaState.hasUnlimitedPhotos) {
                    Text(stringResource(R.string.paywall_ad_limit_reached), color = Gold)
                }
                rewardedAdState.message?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                billingState.message?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Column {
                ElevatedButton(
                    onClick = onBuyUnlimited,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Rose,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(R.string.paywall_buy_unlimited, billingState.price))
                }
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedButton(
                    onClick = onWatchAd,
                    enabled = quotaState.canEarnAdReward && rewardedAdState.isReady,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Gold,
                        contentColor = Color.Black,
                    ),
                ) {
                    val label = if (rewardedAdState.isLoading) {
                        stringResource(R.string.paywall_ad_loading)
                    } else {
                        stringResource(R.string.paywall_watch_ad, PHOTOS_PER_REWARDED_AD)
                    }
                    Text(label)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.paywall_skip))
            }
        },
    )
}
