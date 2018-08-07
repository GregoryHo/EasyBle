package com.ns.greg.ble.internal

import android.bluetooth.BluetoothAdapter
import com.ns.greg.ble.BtManager
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author gregho
 * @since 2018/7/19
 */
internal abstract class BaseBleScanner(
  internal val uuids: Array<UUID>?
) {

  constructor() : this(null)

  private val atomicScanning by lazy(NONE) {
    AtomicBoolean(false)
  }
  internal val bluetoothAdapter: BluetoothAdapter? by lazy {
    BtManager.bluetoothAdapter
  }

  fun startScan(): Boolean {
    if (atomicScanning.compareAndSet(false, true)) {
      startScanImp()
      return true
    }

    return false
  }

  fun stopScan(): Boolean {
    if (atomicScanning.compareAndSet(true, false)) {
      stopScanImp()
      return true
    }

    return false
  }

  internal abstract fun startScanImp()

  internal abstract fun stopScanImp()
}