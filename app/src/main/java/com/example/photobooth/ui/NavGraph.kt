package com.example.photobooth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photobooth.ui.screens.CaptureScreen
import com.example.photobooth.ui.screens.FrameDesignerScreen
import com.example.photobooth.ui.screens.GalleryScreen
import com.example.photobooth.ui.screens.HomeScreen
import com.example.photobooth.ui.screens.SettingsScreen
import com.example.photobooth.ui.screens.TutorialScreen

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
    val startDestination = if (showTutorialOnStart) Screen.Tutorial.route else Screen.Home.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartCapture = { navController.navigate(Screen.Capture.route) },
                onOpenGallery = { navController.navigate(Screen.Gallery.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenTutorial = { navController.navigate(Screen.Tutorial.route) },
                onDonate = { uriHandler.openUri("https://buymeacoffee.com/charleshartmann") },
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
            )
        }
        composable(Screen.FrameDesigner.route) {
            FrameDesignerScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
