package com.charles.photobooth.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

data class BluetoothDeviceInfo(val name: String, val address: String)

@Composable
fun BluetoothDevicePickerDialog(
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            devices = getPairedDevices(context)
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        if (hasBluetoothPermission(context)) {
            devices = getPairedDevices(context)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Select Bluetooth Printer", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                when {
                    permissionDenied -> {
                        Text(
                            "Bluetooth permission is required to scan for printers. " +
                                "Please grant it in app settings.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    devices.isEmpty() -> {
                        Text(
                            "No paired Bluetooth devices found.\n\nPair your thermal printer in Android " +
                                "Bluetooth settings first, then come back here.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                    else -> {
                        Text(
                            "Paired devices — tap to select your printer:",
                            color = Color.Gray,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn {
                            items(devices) { device ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onDeviceSelected(device) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        painter = painterResource(android.R.drawable.stat_sys_data_bluetooth),
                                        contentDescription = null,
                                        tint = Color.Gray,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            device.name.ifBlank { "Unknown device" },
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                        )
                                        Text(
                                            device.address,
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun hasBluetoothPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        Manifest.permission.BLUETOOTH
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun getPairedDevices(context: Context): List<BluetoothDeviceInfo> {
    return try {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = btManager?.adapter ?: return emptyList()
        adapter.bondedDevices.map { device ->
            BluetoothDeviceInfo(
                name = device.name ?: "",
                address = device.address,
            )
        }.sortedBy { it.name }
    } catch (_: SecurityException) {
        emptyList()
    }
}
