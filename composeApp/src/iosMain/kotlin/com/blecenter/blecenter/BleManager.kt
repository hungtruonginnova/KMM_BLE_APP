package com.blecenter.blecenter

import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BluetoothPeripheral
import dev.bluefalcon.BluetoothCharacteristic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

actual class BleManager {
    private var blueFalcon: BlueFalcon? = null
    private var callback: BleManagerCallback? = null
    private val peripheralMap = mutableMapOf<String, BluetoothPeripheral>()
    private var isScanning = false

    init {
        try {
            blueFalcon = BlueFalcon(log = null, null)
            // Start monitoring for discovered peripherals
            startMonitoringPeripherals()
        } catch (e: Exception) {
            callback?.onError("Failed to initialize Blue Falcon: ${e.message}")
        }
    }

    private fun startMonitoringPeripherals() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                if (isScanning) {
                    blueFalcon?.let { bf ->
                        // Check for new peripherals in Blue Falcon's internal list
                        val currentPeripherals = bf.peripherals ?: emptyList()
                        currentPeripherals.forEach { peripheral ->
                            val address = peripheral.uuid
                            if (!peripheralMap.containsKey(address)) {
                                peripheralMap[address] = peripheral
                                val device = toDevice(peripheral)
                                callback?.onDeviceFound(device)
                            } else {
                                // Update existing peripheral info
                                val existingDevice = peripheralMap[address]
                                if (existingDevice != peripheral) {
                                    peripheralMap[address] = peripheral
                                    val device = toDevice(peripheral)
                                    callback?.onDeviceFound(device)
                                }
                            }
                        }
                    }
                }
                delay(500) // Check every 500ms
            }
        }
    }

    actual fun setCallback(callback: BleManagerCallback?) {
        this.callback = callback
    }

    actual fun startScan() {
        try {
            isScanning = true
            peripheralMap.clear()
            blueFalcon?.scan()
        } catch (e: Exception) {
            isScanning = false
            callback?.onError("Failed to start scan: ${e.message}")
        }
    }

    actual fun stopScan() {
        try {
            isScanning = false
            blueFalcon?.stopScanning()
        } catch (e: Exception) {
            callback?.onError("Failed to stop scan: ${e.message}")
        }
    }

    actual fun connect(device: Device) {
        val peripheral = peripheralMap[device.address]
        if (peripheral == null) {
            callback?.onError("Device not found: ${device.address}")
            return
        }
        try {
            blueFalcon?.connect(peripheral, autoConnect = false)
            // Monitor connection status
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                if (peripheral.isConnected) {
                    val updatedDevice = toDevice(peripheral)
                    callback?.onDeviceConnected(updatedDevice)
                }
            }
        } catch (e: Exception) {
            callback?.onError("Failed to connect: ${e.message}")
        }
    }

    actual fun disconnect(device: Device) {
        val peripheral = peripheralMap[device.address]
        if (peripheral == null) {
            callback?.onError("Device not found: ${device.address}")
            return
        }
        try {
            blueFalcon?.disconnect(peripheral)
            val updatedDevice = toDevice(peripheral)
            callback?.onDeviceDisconnected(updatedDevice)
        } catch (e: Exception) {
            callback?.onError("Failed to disconnect: ${e.message}")
        }
    }

    actual fun readCharacteristic(device: Device, serviceUuid: String, characteristicUuid: String) {
        val peripheral = peripheralMap[device.address]
        if (peripheral == null) {
            callback?.onError("Device not found: ${device.address}")
            return
        }
        try {
            val service = peripheral.services?.find { it.uuid == serviceUuid }
            val characteristic = service?.characteristics?.find { it.uuid == characteristicUuid }
            if (characteristic != null) {
                blueFalcon?.readCharacteristic(peripheral, characteristic)
                // Monitor for characteristic value update
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200)
                    val value = characteristic.value ?: ByteArray(0)
                    callback?.onCharacteristicRead(device, serviceUuid, characteristicUuid, value)
                }
            } else {
                callback?.onError("Characteristic not found: $characteristicUuid")
            }
        } catch (e: Exception) {
            callback?.onError("Failed to read characteristic: ${e.message}")
        }
    }

    actual fun writeCharacteristic(device: Device, serviceUuid: String, characteristicUuid: String, value: ByteArray) {
        val peripheral = peripheralMap[device.address]
        if (peripheral == null) {
            callback?.onError("Device not found: ${device.address}")
            return
        }
        try {
            val service = peripheral.services?.find { it.uuid == serviceUuid }
            val characteristic = service?.characteristics?.find { it.uuid == characteristicUuid }
            if (characteristic != null) {
                blueFalcon?.writeCharacteristic(peripheral, characteristic, value.decodeToString())
                callback?.onCharacteristicWrite(device, serviceUuid, characteristicUuid)
            } else {
                callback?.onError("Characteristic not found: $characteristicUuid")
            }
        } catch (e: Exception) {
            callback?.onError("Failed to write characteristic: ${e.message}")
        }
    }

    actual fun notifyCharacteristic(device: Device, serviceUuid: String, characteristicUuid: String, enable: Boolean) {
        val peripheral = peripheralMap[device.address]
        if (peripheral == null) {
            callback?.onError("Device not found: ${device.address}")
            return
        }
        try {
            val service = peripheral.services?.find { it.uuid == serviceUuid }
            val characteristic = service?.characteristics?.find { it.uuid == characteristicUuid }
            if (characteristic != null) {
                blueFalcon?.notifyCharacteristic(peripheral, characteristic, enable)
            } else {
                callback?.onError("Characteristic not found: $characteristicUuid")
            }
        } catch (e: Exception) {
            callback?.onError("Failed to set notification: ${e.message}")
        }
    }

    // Helper method to convert BluetoothPeripheral to Device
    private fun toDevice(peripheral: BluetoothPeripheral): Device {
        return Device(
            name = peripheral.name ?: "Unknown",
            address = peripheral.uuid,
            rssi = peripheral.rssi?.toInt(),
            isConnected = peripheral.isConnected
        )
    }

    // Method to handle discovered peripherals (called when scan finds devices)
    fun onPeripheralDiscovered(peripheral: BluetoothPeripheral) {
        peripheralMap[peripheral.uuid] = peripheral
        val device = toDevice(peripheral)
        callback?.onDeviceFound(device)
    }
}