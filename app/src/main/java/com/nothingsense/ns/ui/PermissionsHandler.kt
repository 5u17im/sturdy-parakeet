package com.nothingsense.ns.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NearbyPermissionsHandler(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    var showHardwareDialog by remember { mutableStateOf(false) }
    var hardwareMessage by remember { mutableStateOf("") }

    val isBluetoothEnabled = remember {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter?.isEnabled ?: false
    }

    val isLocationEnabled = remember {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    LaunchedEffect(permissionState.allPermissionsGranted, isBluetoothEnabled, isLocationEnabled) {
        if (permissionState.allPermissionsGranted) {
            if (!isBluetoothEnabled) {
                hardwareMessage = "Bluetooth is required for mesh networking. Please enable it."
                showHardwareDialog = true
            } else if (!isLocationEnabled) {
                hardwareMessage = "Location services are required for discovery. Please enable GPS."
                showHardwareDialog = true
            } else {
                onPermissionsGranted()
            }
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    if (showHardwareDialog) {
        AlertDialog(
            onDismissRequest = { showHardwareDialog = false },
            title = { Text("Hardware Required") },
            text = { Text(hardwareMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showHardwareDialog = false
                    val intent = if (hardwareMessage.contains("Bluetooth")) {
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    } else {
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHardwareDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
