package com.ns.greg.ble.interfacies

import android.bluetooth.BluetoothGattCharacteristic

/**
 * @author gregho
 * @since 2018/6/11
 */
interface BleConnection {

  fun subscribe(observer: BleConnectionObserver)

  fun unsubscribe()

  fun open()

  fun disconnect()

  fun close()

  fun discoverServices()

  fun read(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  )

  fun write(
    characteristic: BluetoothGattCharacteristic,
    data: ByteArray,
    delayTime: Long
  )

  fun notify(
    characteristic: BluetoothGattCharacteristic,
    enable: Boolean,
    delayTime: Long
  )
}