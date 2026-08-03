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
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nothingsense.ns.R

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
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.RECORD_AUDIO
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
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

    val bluetoothMessage = stringResource(R.string.enable_bluetooth)
    val locationMessage = stringResource(R.string.enable_location)

    LaunchedEffect(permissionState.allPermissionsGranted, isBluetoothEnabled, isLocationEnabled) {
        if (permissionState.allPermissionsGranted) {
            if (!isBluetoothEnabled) {
                hardwareMessage = bluetoothMessage
                showHardwareDialog = true
            } else if (!isLocationEnabled) {
                hardwareMessage = locationMessage
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
            title = { Text(stringResource(R.string.hardware_required)) },
            text = { Text(hardwareMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showHardwareDialog = false
                    val intent = if (hardwareMessage.contains("Bluetooth") || hardwareMessage.contains("mesh")) {
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    } else {
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHardwareDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
