package com.ns.greg.ble.internal

import android.annotation.TargetApi
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import android.support.annotation.RequiresPermission
import java.util.UUID

/**
 * @author gregho
 * @since 2018/7/19
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class BleScanner21(
  uuids: Array<UUID>?,
  private val scanCallback: ScanCallback
) : BaseBleScanner(uuids) {

  constructor(scanCallback: ScanCallback) : this(null, scanCallback)

  @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADMIN)
  override fun startScanImp() {
    val filters = uuids?.run {
      val temp = ArrayList<ScanFilter>()
      for (uuid in this) {
        temp.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(uuid)).build())
      }
      temp
    } ?: run {
      null
    }

    bluetoothAdapter?.bluetoothLeScanner?.startScan(
        filters, ScanSettings.Builder().build(), scanCallback
    )
  }

  @RequiresPermission(android.Manifest.permission.BLUETOOTH_ADMIN)
  override fun stopScanImp() {
    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
  }
}