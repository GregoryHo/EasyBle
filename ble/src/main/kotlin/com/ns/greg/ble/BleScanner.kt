package com.ns.greg.ble

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Build.VERSION_CODES
import com.ns.greg.ble.interfacies.BleScanListener
import com.ns.greg.ble.internal.BleScanner17
import com.ns.greg.ble.internal.BleScanner21
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * @author gregho
 * @since 2018/8/7
 */
open class BleScanner(
  uuids: Array<UUID>?,
  private val bleScanListener: BleScanListener
) {

  constructor(bleScanListener: BleScanListener) : this(null, bleScanListener)

  private val scanner by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      BleScanner17(
          uuids,
          SimpleLeScanCallback(this)
      )
    } else {
      BleScanner21(
          uuids,
          SimpleScanCallback(this)
      )
    }
  }

  fun startScan(scanPeriod: Long) {
    if (scanner.startScan()) {
      bleScanListener.onStartScan()
      if (scanPeriod > 0) {
        launch(CommonPool) {
          delay(scanPeriod)
          stopScan()
        }
      }
    }
  }

  fun stopScan() {
    if (scanner.stopScan()) {
      bleScanListener.onStopScan()
    }
  }

  private fun onScan(
    device: BluetoothDevice?,
    rssi: Int,
    scanRecord: ByteArray?
  ) {
    device?.run {
      bleScanListener.onScan(this, rssi, scanRecord)
    }
  }

  class SimpleLeScanCallback(bleWhiteBoxScanner: BleScanner) : LeScanCallback {

    private val instance: BleScanner by lazy {
      WeakReference<BleScanner>(bleWhiteBoxScanner).get()!!
    }

    override fun onLeScan(
      device: BluetoothDevice?,
      rssi: Int,
      scanRecord: ByteArray?
    ) {
      instance.onScan(device, rssi, scanRecord)
    }
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  class SimpleScanCallback(bleWhiteBoxScanner: BleScanner) : ScanCallback() {

    private val instance: BleScanner by lazy {
      WeakReference<BleScanner>(bleWhiteBoxScanner).get()!!
    }

    override fun onScanResult(
      callbackType: Int,
      result: ScanResult?
    ) {
      result?.run {
        instance.onScan(device, rssi, scanRecord.bytes)
      }
    }
  }
}