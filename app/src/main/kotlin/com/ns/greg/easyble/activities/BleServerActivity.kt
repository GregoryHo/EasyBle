package com.ns.greg.easyble.activities

import android.annotation.TargetApi
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.Toast
import com.ns.greg.ble.BtManager
import com.ns.greg.ble.interfacies.BluetoothListener
import com.ns.greg.easyble.BleDemoServer

/**
 * @author gregho
 * @since 2018/7/24
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
class BleServerActivity : AppCompatActivity() {

  private var bleWhiteBoxMockServer: BleDemoServer? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!BtManager.isBluetoothSupported()) {
      Toast.makeText(applicationContext, "Device not support Bluetooth.", Toast.LENGTH_SHORT)
          .show()
      finish()
    } else if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
      Toast.makeText(applicationContext, "Device api not support BLE server.", Toast.LENGTH_SHORT)
          .show()
      finish()
    }

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  override fun onResume() {
    super.onResume()
    checkBluetooth()
  }

  private fun checkBluetooth() {
    if (BtManager.isBluetoothEnabled()) {
      openServer()
    } else {
      BtManager.enableBluetooth(applicationContext, object : BluetoothListener {
        override fun onAccepted() {
          Toast.makeText(applicationContext, "Bluetooth is enabled.", Toast.LENGTH_SHORT)
              .show()
        }

        override fun onDenied() {
          Toast.makeText(applicationContext, "Bluetooth is not enabled.", Toast.LENGTH_SHORT)
              .show()
          finish()
        }
      })
    }
  }

  private fun openServer() {
    if (bleWhiteBoxMockServer == null) {
      bleWhiteBoxMockServer = BleDemoServer(applicationContext, true)
    }

    bleWhiteBoxMockServer?.open()
  }

  override fun onPause() {
    super.onPause()
    closeServer()
  }

  private fun closeServer() {
    bleWhiteBoxMockServer?.close()
  }
}