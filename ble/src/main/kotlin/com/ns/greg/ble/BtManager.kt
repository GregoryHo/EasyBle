package com.ns.greg.ble

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.support.annotation.RequiresPermission
import com.ns.greg.ble.interfacies.BluetoothListener

/**
 * @author gregho
 * @since 2018/8/6
 */
class BtManager {

  companion object {

    @JvmStatic
    internal var listener: BluetoothListener? = null
    internal val bluetoothAdapter: BluetoothAdapter? by lazy {
      BluetoothAdapter.getDefaultAdapter()
    }

    fun isBluetoothSupported(): Boolean {
      return bluetoothAdapter != null
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH)
    fun isBluetoothEnabled(): Boolean {
      return bluetoothAdapter?.isEnabled ?: false
    }

    fun enableBluetooth(
      context: Context,
      listener: BluetoothListener
    ) {
      this.listener = listener
      val intent = Intent(context, BtRequestActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    }
  }
}