package com.ns.greg.ble.interfacies

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import java.net.UnknownServiceException
import java.util.UUID

/**
 * @author gregho
 * @since 2018/7/20
 */
interface BleServerObserver {

  fun onConnected(client: BluetoothDevice)

  fun onDisconnected(client: BluetoothDevice)

  @Throws(UnknownServiceException::class)
  fun onServiceAdded(
    service: BluetoothGattService?,
    e: Exception?
  )

  @Throws(IllegalAccessException::class)
  fun onWriteRequest(
    device: BluetoothDevice,
    uuid: UUID,
    e: Exception?
  ): ByteArray?

  fun onWriteFinished(
    device: BluetoothDevice,
    uuid: UUID,
    data: ByteArray
  )

  @Throws(IllegalAccessException::class)
  fun onReadRequest(
    device: BluetoothDevice,
    uuid: UUID,
    e: Exception?
  )

  fun onReadFinished(
    device: BluetoothDevice,
    uuid: UUID
  )
}