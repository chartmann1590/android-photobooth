package com.charles.photobooth.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.charles.photobooth.monetization.MonetizationViewModel
import com.charles.photobooth.ui.screens.CaptureScreen
import com.charles.photobooth.ui.screens.FrameDesignerScreen
import com.charles.photobooth.ui.screens.GalleryScreen
import com.charles.photobooth.ui.screens.HomeScreen
import com.charles.photobooth.ui.screens.SettingsScreen
import com.charles.photobooth.ui.screens.TutorialScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Capture : Screen("capture")
    data object Gallery : Screen("gallery")
    data object Settings : Screen("settings")
    data object FrameDesigner : Screen("frame_designer")
    data object Tutorial : Screen("tutorial")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    showTutorialOnStart: Boolean = false,
    onTutorialSeen: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val activity = LocalContext.current.findActivity()
    val monetizationViewModel: MonetizationViewModel = viewModel()
    val quotaState by monetizationViewModel.quotaState.collectAsStateWithLifecycle()
    val rewardedAdState by monetizationViewModel.rewardedAdState.collectAsStateWithLifecycle()
    val billingState by monetizationViewModel.billingState.collectAsStateWithLifecycle()
    val startDestination = if (showTutorialOnStart) Screen.Tutorial.route else Screen.Home.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartCapture = { navController.navigate(Screen.Capture.route) },
                onOpenGallery = { navController.navigate(Screen.Gallery.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenTutorial = { navController.navigate(Screen.Tutorial.route) },
                quotaState = quotaState,
                rewardedAdState = rewardedAdState,
                billingState = billingState,
                onWatchRewardedAd = { activity?.let(monetizationViewModel::watchRewardedAd) },
                onBuyUnlimited = { activity?.let(monetizationViewModel::buyUnlimited) },
            )
        }
        composable(Screen.Tutorial.route) {
            TutorialScreen(
                onBack = {
                    onTutorialSeen()
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Tutorial.route) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Screen.Capture.route) {
            CaptureScreen(
                onBack = { navController.popBackStack() },
                onFinishedCapture = { _ ->
                    navController.navigate(
                        Screen.Gallery.route,
                        NavOptions.Builder()
                            .setPopUpTo(Screen.Capture.route, inclusive = true)
                            .build(),
                    )
                },
                quotaState = quotaState,
                rewardedAdState = rewardedAdState,
                billingState = billingState,
                onReservePhotos = monetizationViewModel::reservePhotos,
                onRefundPhotos = monetizationViewModel::refundPhotos,
                onWatchRewardedAd = { activity?.let(monetizationViewModel::watchRewardedAd) },
                onBuyUnlimited = { activity?.let(monetizationViewModel::buyUnlimited) },
            )
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenFrameDesigner = { navController.navigate(Screen.FrameDesigner.route) },
                onOpenWebsite = { uriHandler.openUri("https://chartmann1590.github.io/android-photobooth/") },
            )
        }
        composable(Screen.FrameDesigner.route) {
            FrameDesignerScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
