package com.ns.greg.ble.internal

import android.util.Log
import com.ns.greg.ble.BuildConfig

/**
 * @author gregho
 * @since 2018/6/26
 */
class BleLogger {

  companion object Functions {

    private const val TAG = "BleLogger"
    private const val LEFT_BRACKET = "["
    private const val RIGHT_BRACKET = "]"
    private const val SIGN = "-->"
    private const val LINK = "└--"
    private var debug = false

    @JvmStatic
    fun enableLogger() {
      debug = true
    }

    @JvmStatic
    @JvmOverloads
    fun log(
      className: String = "",
      functionName: String = "",
      message: String = ""
    ) {
      if (BuildConfig.DEBUG && debug) {
        if (className.isNotEmpty()) {
          Log.d(TAG, "$LEFT_BRACKET$className$RIGHT_BRACKET")
        }

        if (functionName.isNotEmpty()) {
          Log.d(TAG, "$SIGN $functionName")
        }

        if (message.isNotEmpty()) {
          Log.d(TAG, "$LINK $message")
        }
      }
    }
  }
}