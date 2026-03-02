package com.example.photobooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.example.photobooth.ui.NavGraph
import com.example.photobooth.ui.theme.DarkBackground
import com.example.photobooth.ui.theme.PhotoboothTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PhotoboothTheme {
                Surface(color = DarkBackground) {
                    NavGraph()
                }
            }
        }
    }
}
