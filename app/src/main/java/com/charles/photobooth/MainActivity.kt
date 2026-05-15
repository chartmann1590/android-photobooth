package com.charles.photobooth

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.charles.photobooth.PhotoboothApp
import com.charles.photobooth.R
import com.charles.photobooth.ui.NavGraph
import com.charles.photobooth.ui.screens.ConsentDialog
import com.charles.photobooth.ui.theme.DarkBackground
import com.charles.photobooth.ui.theme.Gold
import com.charles.photobooth.ui.theme.PhotoboothTheme
import com.charles.photobooth.ui.theme.Rose
import com.charles.photobooth.ui.theme.TextSecondary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Exception) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        setContent {
            PhotoboothTheme {
                Surface(color = DarkBackground) {
                    MainEntry()
                }
            }
        }
    }
}

@Composable
private fun MainEntry() {
    val context = LocalContext.current
    val app = remember { context.applicationContext as PhotoboothApp }
    val scope = rememberCoroutineScope()
    var needsConsent by remember { mutableStateOf(false) }
    var consentChecked by remember { mutableStateOf(false) }
    var tutorialChecked by remember { mutableStateOf(false) }
    var showTutorialOnStart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        needsConsent = app.needsConsent()
        if (!needsConsent) {
            showTutorialOnStart = !app.hasSeenTutorial()
            tutorialChecked = true
        }
        consentChecked = true
    }

    when {
        !consentChecked -> {}
        needsConsent -> {
            ConsentDialog(onDecision = {
                needsConsent = false
                tutorialChecked = false
            })
        }
        else -> {
            LaunchedEffect(needsConsent, tutorialChecked) {
                if (!needsConsent && !tutorialChecked) {
                    showTutorialOnStart = !app.hasSeenTutorial()
                    tutorialChecked = true
                }
            }
            PermissionGate {
                if (tutorialChecked) {
                    NavGraph(
                        showTutorialOnStart = showTutorialOnStart,
                        onTutorialSeen = {
                            showTutorialOnStart = false
                            scope.launch { app.setTutorialSeen() }
                        },
                    )
                }
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    val perms = mutableListOf(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return perms.toTypedArray()
}

private fun allGranted(context: android.content.Context): Boolean {
    return requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(allGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = allGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
    }

    if (granted) {
        content()
    } else {
        PermissionDeniedScreen(
            onRequestPermission = { launcher.launch(requiredPermissions()) },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
        )
    }
}

@Composable
private fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_camera),
            contentDescription = null,
            tint = Rose,
            modifier = Modifier.size(72.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.permissions_required),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permissions_rationale),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        ElevatedButton(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Rose,
                contentColor = Color.White,
            ),
        ) {
            Text(stringResource(R.string.grant_permissions), fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(12.dp))
        ElevatedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Gold,
                contentColor = Color.Black,
            ),
        ) {
            Text(stringResource(R.string.open_settings), fontWeight = FontWeight.Medium)
        }
    }
}
