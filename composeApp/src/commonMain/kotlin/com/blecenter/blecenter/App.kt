package com.blecenter.blecenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blecenter.blecenter.ui.theme.BleCenterTheme

// BLE device data class compatible with Blue Falcon
data class Device(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val isConnected: Boolean = false
)

@Composable
@Preview
fun App(bleManager: BleManager) {
    BleCenterTheme {
        var isScanning by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val devices = remember { mutableStateListOf<Device>() }
        val connectedDevices = remember { mutableSetOf<String>() }

        // Setup callback
        LaunchedEffect(bleManager) {
            bleManager.setCallback(object : BleManagerCallback {
                override fun onDeviceFound(device: Device) {
                    val existingIndex = devices.indexOfFirst { it.address == device.address }
                    if (existingIndex >= 0) {
                        devices[existingIndex] = device
                    } else {
                        devices.add(device)
                    }
                }

                override fun onDeviceConnected(device: Device) {
                    connectedDevices.add(device.address)
                    val index = devices.indexOfFirst { it.address == device.address }
                    if (index >= 0) {
                        devices[index] = device.copy(isConnected = true)
                    }
                }

                override fun onDeviceDisconnected(device: Device) {
                    connectedDevices.remove(device.address)
                    val index = devices.indexOfFirst { it.address == device.address }
                    if (index >= 0) {
                        devices[index] = device.copy(isConnected = false)
                    }
                }

                override fun onCharacteristicRead(
                    device: Device,
                    serviceUuid: String,
                    characteristicUuid: String,
                    value: ByteArray
                ) {
                    // Handle characteristic read
                }

                override fun onCharacteristicWrite(
                    device: Device,
                    serviceUuid: String,
                    characteristicUuid: String
                ) {
                    // Handle characteristic write
                }

                override fun onError(error: String) {
                    errorMessage = error
                }
            })
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                isScanning = !isScanning
                if (isScanning) {
                    devices.clear()
                    bleManager.startScan()
                } else {
                    bleManager.stopScan()
                }
            }) {
                Text(if (isScanning) "Stop Scan" else "Start Scan")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = if (isScanning) "Scanning..." else "Not scanning")

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No devices found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(devices) { device ->
                        DeviceListItem(
                            device = device,
                            onConnectClick = {
                                if (device.isConnected) {
                                    bleManager.disconnect(device)
                                } else {
                                    bleManager.connect(device)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceListItem(device: Device, onConnectClick: (Device) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = device.address, style = MaterialTheme.typography.bodyMedium)
                device.rssi?.let { rssi ->
                    Text(text = "RSSI: $rssi dBm", style = MaterialTheme.typography.bodySmall)
                }
                if (device.isConnected) {
                    Text(
                        text = "Connected",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Button(onClick = { onConnectClick(device) }) {
                Text(if (device.isConnected) "Disconnect" else "Connect")
            }
        }
    }
}