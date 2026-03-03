package com.example.photobooth.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photobooth.ui.screens.CaptureScreen
import com.example.photobooth.ui.screens.FrameDesignerScreen
import com.example.photobooth.ui.screens.GalleryScreen
import com.example.photobooth.ui.screens.HomeScreen
import com.example.photobooth.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Capture : Screen("capture")
    data object Gallery : Screen("gallery")
    data object Settings : Screen("settings")
    data object FrameDesigner : Screen("frame_designer")
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartCapture = { navController.navigate(Screen.Capture.route) },
                onOpenGallery = { navController.navigate(Screen.Gallery.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.Capture.route) {
            CaptureScreen(
                onBack = { navController.popBackStack() },
                onFinishedCapture = { photoId ->
                    navController.navigate(Screen.Gallery.route)
                }
            )
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onBack = { navController.popBackStack() }
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
