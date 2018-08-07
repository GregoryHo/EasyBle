package com.ns.greg.ble.internal

/**
 * @author gregho
 * @since 2018/5/31
 */
open class SingletonHolder1<out T, in U>(initializer: (U) -> T) {

  private var initializer: ((U) -> T)? = initializer
  @Volatile
  private var instance: T? = null

  fun init(argument: U) {
    if (instance == null) {
      synchronized(this) {
        if (instance == null) {
          instance = initializer!!(argument)
        }
      }
    }
  }

  @Throws(NullPointerException::class)
  fun getInstance(): T {
    instance?.let {
      return it
    } ?: run {
      throw NullPointerException("Please call init() before getInstance().")
    }
  }
}