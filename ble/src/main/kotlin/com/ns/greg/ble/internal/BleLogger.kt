package com.ns.greg.ble.internal

import android.util.Log
import com.ns.greg.ble.BuildConfig
import com.ns.greg.library.fancy_logger.FancyLogger
import com.ns.greg.library.fancy_logger.Printer

/**
 * @author gregho
 * @since 2018/6/26
 */
internal class BleLogger {

  companion object {

    private const val TAG = "BleLogger"
    private var debug = false

    fun enableLogger() {
      debug = true
    }

    fun log(
      functionName: String,
      message: String = ""
    ) {
      if (BuildConfig.DEBUG && debug) {
        if (functionName.isNotEmpty()) {
          Log.d(TAG, "-> $functionName")
        }

        Log.d(TAG, message)
      }
    }
  }
}