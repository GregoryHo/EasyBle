package com.ns.greg.ble.interfacies

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.ns.greg.ble.ConnectionState

/**
 * @author gregho
 * @since 2018/6/25
 */
interface BleConnectionObserver {

  /**
   * Callback indicating when GATT client has connected/disconnected to/from a remote
   * GATT server.
   *
   * @param state see [ConnectionState] to find out state defines
   *
   */
  fun onConnectionStateChanged(state: ConnectionState)

  /**
   * Callback invoked when the list of remote services, characteristics and descriptors
   * for the remote device have been updated, ie new services have been discovered.
   *
   * @param status [BluetoothGatt.GATT_SUCCESS] if the remote device
   *               has been explored successfully.
   */
  fun onDiscovered(status: Int)

  /**
   * Callback indicating the result of a characteristic read operation
   *
   * @param characteristic Characteristic that was read from the associated
   *                       remote device.
   * @param data The data read form the remote GATT server when succeeds.
   */
  fun onRead(
    characteristic: BluetoothGattCharacteristic,
    data: ByteArray?
  )

  /**
   * Callback indicating the result of a characteristic write operation.
   *
   * @param characteristic Characteristic that was written to the associated
   *                       remote device.
   * @param status [BluetoothGatt.GATT_SUCCESS] if the operation succeeds.
   */
  fun onWrite(
    characteristic: BluetoothGattCharacteristic,
    status: Int
  )
}