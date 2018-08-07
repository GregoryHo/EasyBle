package com.ns.greg.ble.interfacies

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import com.ns.greg.ble.ConnectionState
import java.util.UUID

/**
 * @author gregho
 * @since 2018/6/11
 */
interface BleDevice {

  fun getContext(): Context

  fun getBluetoothDevice(): BluetoothDevice

  fun createConnection(autoConnect: Boolean): BleConnection

  fun getConnectionState(): ConnectionState

  fun setServices(services: List<BluetoothGattService>)

  fun getServices(): List<BluetoothGattService>

  fun getService(serviceUuid: UUID): BluetoothGattService?

  fun getCharacteristics(serviceUuid: UUID): List<BluetoothGattCharacteristic>?

  fun getCharacteristic(
    serviceUuid: UUID,
    characteristicUuid: UUID
  ): BluetoothGattCharacteristic?
}