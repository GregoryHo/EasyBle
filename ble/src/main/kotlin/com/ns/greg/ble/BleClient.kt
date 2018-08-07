package com.ns.greg.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.ns.greg.ble.interfacies.BleDevice
import com.ns.greg.ble.internal.BleDeviceImp

/**
 * @author gregho
 * @since 2018/6/22
 */
class BleClient constructor(
  context: Context,
  device: BluetoothDevice
) {

  private val device: BleDeviceImp by lazy {
    BleDeviceImp(context, device)
  }

  fun getDevice(): BleDevice {
    return device
  }
}