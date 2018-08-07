package com.ns.greg.ble.interfacies

import android.bluetooth.BluetoothDevice

/**
 * @author gregho
 * @since 2018/7/19
 */
interface BleScanListener {

  fun onStartScan()

  fun onScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?)

  fun onStopScan()
}