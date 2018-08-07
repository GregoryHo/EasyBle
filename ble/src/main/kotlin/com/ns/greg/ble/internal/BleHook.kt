package com.ns.greg.ble.internal

import com.ns.greg.library.fasthook.BaseRunnable
import com.ns.greg.library.fasthook.BaseThreadManager
import com.ns.greg.library.fasthook.BaseThreadTask
import com.ns.greg.library.fasthook.ThreadExecutorFactory
import java.util.concurrent.ThreadPoolExecutor

/**
 * @author gregho
 * @since 2018/5/31
 */
internal class BleHook private constructor() : BaseThreadManager<ThreadPoolExecutor>() {

  companion object {

    // Singleton instance
    val instance: BleHook by lazy {
      BleHook()
    }
  }

  init {
    setLog(false)
  }

  override fun createBaseThreadTask(job: BaseRunnable<*>?): BaseThreadTask {
    return BaseThreadTask(job)
  }

  override fun createThreadPool(): ThreadPoolExecutor {
    return ThreadExecutorFactory.newSingleThreadExecutor()
  }
}