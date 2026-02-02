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

// Placeholder for a BLE device
data class Device(val name: String, val address: String)

@Composable
@Preview
fun App(bleManager: BleManager) {
    BleCenterTheme {
        var isScanning by remember { mutableStateOf(false) }
        // Placeholder for the list of found devices
        val devices = remember { mutableStateListOf<Device>() }

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
                    bleManager.startScan()
                    // Simulate finding devices for preview
                    if (bleManager.toString().contains("FakeBleManager")) {
                        devices.add(Device("Device 1", "00:11:22:33:FF:DD"))
                        devices.add(Device("Device 2", "00:11:22:33:FF:DE"))
                    }
                } else {
                    bleManager.stopScan()
                    devices.clear()
                }
            }) {
                Text(if (isScanning) "Stop Scan" else "Start Scan")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = if (isScanning) "Scanning..." else "Not scanning")

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
                        DeviceListItem(device = device, onConnectClick = {
                            // Handle connect click
                        })
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
            Column {
                Text(text = device.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = device.address, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { onConnectClick(device) }) {
                Text("Connect")
            }
        }
    }
}