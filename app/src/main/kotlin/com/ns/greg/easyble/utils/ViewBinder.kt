package com.ns.greg.easyble.utils

import android.app.Activity
import android.support.annotation.IdRes
import android.view.View
import android.view.ViewGroup
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author gregho
 * @since 2018/6/1
 */
class ViewBinder {

  companion object {

    fun <T : View> bind(parent: ViewGroup, @IdRes id: Int): Lazy<T> {
      return asyncLazy { parent.findViewById(id) as T }
    }

    fun <T : View> bind(parent: Activity, @IdRes id: Int): Lazy<T> {
      return asyncLazy { parent.findViewById(id) as T }
    }

    private fun <T> asyncLazy(initializer: () -> T) = lazy(NONE, initializer)
  }
}