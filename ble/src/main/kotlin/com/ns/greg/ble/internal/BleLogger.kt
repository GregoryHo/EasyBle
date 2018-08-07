package com.ns.greg.ble.internal

import com.ns.greg.ble.BuildConfig
import com.ns.greg.library.fancy_logger.FancyLogger
import com.ns.greg.library.fancy_logger.Printer

/**
 * @author gregho
 * @since 2018/6/26
 */
internal class BleLogger {

  companion object {

    init {
      if (BuildConfig.DEBUG) {
        FancyLogger.add(
            FancyLogger.LOW_PRIORITY,
            Printer.Builder().showThreadInfo(false).setMethodCount(0).build()
        )
      }
    }

    private const val TAG = "BleLogger"

    fun log(message: String) {
      if (BuildConfig.DEBUG) {
        FancyLogger.e(TAG, message)
      }
    }
  }
}