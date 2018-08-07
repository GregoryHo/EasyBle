package com.ns.greg.ble.internal

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.os.Build
import android.support.annotation.RequiresPermission
import java.util.UUID

/**
 * @author gregho
 * @since 2018/7/19
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
internal class BleScanner17(
  uuids: Array<UUID>?,
  private val scanCallback: LeScanCallback
) : BaseBleScanner(uuids) {

  constructor(scanCallback: LeScanCallback) : this(null, scanCallback)

  @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADMIN)
  override fun startScanImp() {
    bluetoothAdapter?.startLeScan(uuids, scanCallback)
  }

  @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADMIN)
  override fun stopScanImp() {
    bluetoothAdapter?.stopLeScan(scanCallback)
  }
}