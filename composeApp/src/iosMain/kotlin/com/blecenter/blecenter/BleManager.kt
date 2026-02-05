@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.blecenter.blecenter

import dev.bluefalcon.AdvertisementDataRetrievalKeys
import dev.bluefalcon.BlueFalcon
import dev.bluefalcon.BlueFalconDelegate
import dev.bluefalcon.BluetoothCharacteristic
import dev.bluefalcon.BluetoothCharacteristicDescriptor
import dev.bluefalcon.BluetoothPeripheral
import dev.bluefalcon.BluetoothPeripheralState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication

actual class BleManager : BlueFalconDelegate {

    private var blueFalcon: BlueFalcon? = null
    private var callback: BleManagerCallback? = null
    private val peripheralMap = mutableMapOf<String, BluetoothPeripheral>()
    private var isScanning = false
    private val scope = MainScope()

    init {
        try {
            blueFalcon = BlueFalcon(log = null, UIApplication.sharedApplication)
            blueFalcon?.delegates?.add(this)
        } catch (e: Exception) {
            callback?.onError("Failed to initialize Blue Falcon: ${e.message}")
        }
    }

    actual fun setCallback(callback: BleManagerCallback?) {
        this.callback = callback
    }

    actual fun startScan() {
        try {
            isScanning = true
            peripheralMap.clear()
            blueFalcon?.scan(emptyList())
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
        val characteristic = findCharacteristic(peripheral, serviceUuid, characteristicUuid)
        if (characteristic == null) {
            callback?.onError("Characteristic not found: $characteristicUuid")
            return
        }
        try {
            blueFalcon?.readCharacteristic(peripheral, characteristic)
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
        val characteristic = findCharacteristic(peripheral, serviceUuid, characteristicUuid)
        if (characteristic == null) {
            callback?.onError("Characteristic not found: $characteristicUuid")
            return
        }
        try {
            blueFalcon?.writeCharacteristic(peripheral, characteristic, value, 2)
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
        val characteristic = findCharacteristic(peripheral, serviceUuid, characteristicUuid)
        if (characteristic == null) {
            callback?.onError("Characteristic not found: $characteristicUuid")
            return
        }
        try {
            blueFalcon?.notifyCharacteristic(peripheral, characteristic, enable)
        } catch (e: Exception) {
            callback?.onError("Failed to set notification: ${e.message}")
        }
    }

    // ==================== BlueFalconDelegate ====================

    override fun didDiscoverDevice(
        bluetoothPeripheral: BluetoothPeripheral,
        advertisementData: Map<AdvertisementDataRetrievalKeys, Any>
    ) {
        val address = bluetoothPeripheral.uuid
        peripheralMap[address] = bluetoothPeripheral
        val device = toDevice(bluetoothPeripheral)
        scope.launch(Dispatchers.Main) {
            callback?.onDeviceFound(device)
        }
    }

    override fun didConnect(bluetoothPeripheral: BluetoothPeripheral) {
        peripheralMap[bluetoothPeripheral.uuid] = bluetoothPeripheral
        val device = toDevice(bluetoothPeripheral).copy(isConnected = true)
        scope.launch(Dispatchers.Main) {
            callback?.onDeviceConnected(device)
        }
    }

    override fun didDisconnect(bluetoothPeripheral: BluetoothPeripheral) {
        val device = toDevice(bluetoothPeripheral).copy(isConnected = false)
        scope.launch(Dispatchers.Main) {
            callback?.onDeviceDisconnected(device)
        }
    }

    override fun didDiscoverServices(bluetoothPeripheral: BluetoothPeripheral) {
        // Services discovered after connect; no direct callback in BleManagerCallback
    }

    override fun didDiscoverCharacteristics(bluetoothPeripheral: BluetoothPeripheral) {
        // Characteristics discovered; no direct callback in BleManagerCallback
    }

    override fun didCharacteristcValueChanged(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic
    ) {
        val value = bluetoothCharacteristic.value ?: return
        val serviceUuid = findServiceUuid(bluetoothPeripheral, bluetoothCharacteristic) ?: return
        val characteristicUuid = bluetoothCharacteristic.uuid.toString()
        val device = toDevice(bluetoothPeripheral)
        scope.launch(Dispatchers.Main) {
            callback?.onCharacteristicRead(device, serviceUuid, characteristicUuid, value)
        }
    }

    override fun didRssiUpdate(bluetoothPeripheral: BluetoothPeripheral) {
        // Optional: could notify UI of RSSI change
    }

    override fun didUpdateMTU(bluetoothPeripheral: BluetoothPeripheral, status: Int) {
        // MTU updated
    }

    override fun didReadDescriptor(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristicDescriptor: BluetoothCharacteristicDescriptor
    ) {}

    override fun didWriteDescriptor(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristicDescriptor: BluetoothCharacteristicDescriptor
    ) {}

    override fun didWriteCharacteristic(
        bluetoothPeripheral: BluetoothPeripheral,
        bluetoothCharacteristic: BluetoothCharacteristic,
        success: Boolean
    ) {
        if (success) {
            val device = toDevice(bluetoothPeripheral)
            val serviceUuid = findServiceUuid(bluetoothPeripheral, bluetoothCharacteristic) ?: return
            scope.launch(Dispatchers.Main) {
                callback?.onCharacteristicWrite(device, serviceUuid, bluetoothCharacteristic.uuid.toString())
            }
        }
    }

    // ==================== Helpers ====================

    private fun toDevice(peripheral: BluetoothPeripheral): Device {
        val isConnected = blueFalcon?.connectionState(peripheral) == BluetoothPeripheralState.Connected
        return Device(
            name = peripheral.name ?: "Unknown",
            address = peripheral.uuid,
            rssi = peripheral.rssi?.toInt(),
            isConnected = isConnected
        )
    }

    private fun findCharacteristic(
        peripheral: BluetoothPeripheral,
        serviceUuid: String,
        characteristicUuid: String
    ): BluetoothCharacteristic? {
        val service = peripheral.services.values.find { service ->
            service.uuid.toString().equals(serviceUuid, ignoreCase = true)
        } ?: return null
        return service.characteristics.find {
            it.uuid.toString().equals(characteristicUuid, ignoreCase = true)
        }
    }

    private fun findServiceUuid(
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothCharacteristic
    ): String? {
        val charUuidStr = characteristic.uuid.toString()
        for (service in peripheral.services.values) {
            if (service.characteristics.any { it.uuid.toString() == charUuidStr }) {
                return service.uuid.toString()
            }
        }
        return null
    }
}
