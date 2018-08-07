package com.ns.greg.ble.internal.buffer

import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author gregho
 * @since 2018/5/31
 */
internal abstract class BleBuffer {

  companion object {

    const val MAXIMUM_PAYLOAD_SIZE = 19
    const val BYTES_256 = 256
    const val MAXIMUM_SEQUENCE = 127
  }

  private val atomicNoMorFlag: AtomicBoolean = AtomicBoolean(false)
  private var buffer: ByteArray? = null

  constructor() : this(BYTES_256)

  constructor(size: Int) {
    setBuffer(createBuffer(size))
  }

  abstract fun release(cleanBuffer: Boolean)

  protected fun getBuffer(): ByteArray? {
    return buffer
  }

  protected fun setBuffer(buffer: ByteArray?) {
    this.buffer = buffer
  }

  protected fun createBuffer(size: Int): ByteArray? {
    if (size < 0) {
      return null
    }

    val bytes = ByteArray(size)
    Arrays.fill(bytes, 0)
    return bytes
  }

  protected fun isFoundNoMoreFlag(): Boolean {
    return atomicNoMorFlag.get()
  }

  protected fun getAtomicNoMoreFlag(): AtomicBoolean {
    return atomicNoMorFlag
  }

  protected fun setNoMoreFlag(flag: Boolean) {
    this.atomicNoMorFlag.set(flag)
  }

  protected fun wrapBuffer(src: Int, dest: Int, offset: Int) {
    try {
      // Copy current aggregated data to temp array
      val temp = ByteArray(src)
      System.arraycopy(getBuffer(), 0, temp, 0, src)
      // Create new buffer with new size which will plus offset
      setBuffer(createBuffer(dest + offset))
      // Get data back from temp array
      System.arraycopy(temp, 0, getBuffer(), 0, src)
    } catch (e: NullPointerException) {
      e.printStackTrace()
    }
  }
}