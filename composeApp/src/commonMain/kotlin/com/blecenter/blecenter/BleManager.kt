package com.blecenter.blecenter

// Callback interface for BLE events
interface BleManagerCallback {
    fun onDeviceFound(device: Device)
    fun onDeviceConnected(device: Device)
    fun onDeviceDisconnected(device: Device)
    fun onCharacteristicRead(device: Device, serviceUuid: String, characteristicUuid: String, value: ByteArray)
    fun onCharacteristicWrite(device: Device, serviceUuid: String, characteristicUuid: String)
    fun onError(error: String)
}

expect class BleManager {
    fun setCallback(callback: BleManagerCallback?)
    fun startScan()
    fun stopScan()
    fun connect(device: Device)
    fun disconnect(device: Device)
    fun readCharacteristic(device: Device, serviceUuid: String, characteristicUuid: String)
    fun writeCharacteristic(device: Device, serviceUuid: String, characteristicUuid: String, value: ByteArray)
    fun notifyCharacteristic(device: Device, serviceUuid: String, characteristicUuid: String, enable: Boolean)
}