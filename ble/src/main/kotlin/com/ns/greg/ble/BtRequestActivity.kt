package com.ns.greg.ble

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager

/**
 * @author gregho
 * @since 2018/8/6
 */
class BtRequestActivity : Activity() {

  companion object {

    const val CODE_REQUEST_BT = 3412
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    )
    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    startActivityForResult(
        intent,
        CODE_REQUEST_BT
    )
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      CODE_REQUEST_BT -> {
        when (resultCode) {
          Activity.RESULT_OK -> BtManager.listener?.onAccepted()
          else -> BtManager.listener?.onDenied()
        }

        BtManager.listener = null
        finish()
      }
    }
  }
}