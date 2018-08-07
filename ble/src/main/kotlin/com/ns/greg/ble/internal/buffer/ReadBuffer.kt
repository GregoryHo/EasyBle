package com.ns.greg.ble.internal.buffer

import android.util.SparseIntArray
import com.ns.greg.ble.BuildConfig
import com.ns.greg.ble.internal.BleLogger
import com.ns.greg.ble.internal.PacketProcessor

/**
 * @author gregho
 * @since 2018/5/31
 */
internal class ReadBuffer : BleBuffer {

  private var packetSize = 0
  private val sparseSequence = SparseIntArray()

  constructor() : super()
  constructor(size: Int) : super(size)

  override fun release(cleanBuffer: Boolean) {
    packetSize = 0
    setNoMoreFlag(false)
    if (cleanBuffer) {
      setBuffer(createBuffer(BYTES_256))
      sparseSequence.clear()
    }
  }

  fun isAggregated(): Boolean {
    if (!isFoundNoMoreFlag()) {
      BleLogger.log("Not found ended sequence.")
      return false
    }

    var lastSequence = 0
    val length = sparseSequence.size()
    for (i in 0 until length) {
      lastSequence = Math.max(lastSequence, sparseSequence.keyAt(i))
    }

    if (BuildConfig.DEBUG) {
      val missed = StringBuffer(lastSequence / 2)
      val found = StringBuffer(lastSequence)
      for (i in 1 until lastSequence) {
        if (sparseSequence[i] == 0) {
          missed.append("[")
              .append(i)
              .append("]")
        } else {
          found.append("[")
              .append(i)
              .append("]")
        }
      }

      found.append("[")
          .append(lastSequence)
          .append("]")
      BleLogger.log("Missed sequence: " + missed.toString())
      BleLogger.log("Found sequence: " + found.toString())
    }

    return length == lastSequence
  }

  fun getData(): ByteArray {
    val bytes = ByteArray(packetSize)
    try {
      System.arraycopy(getBuffer(), 0, bytes, 0, packetSize)
    } catch (e: NullPointerException) {
      e.printStackTrace()
    }

    return bytes
  }

  fun read(data: ByteArray?) {
    if (data == null) {
      return
    }

    val length = data.size
    if (length <= 0) {
      return
    }

    val header = data[0]
    val sequence = PacketProcessor.getSequence(header)
    // If this sequence is already read
    if (sparseSequence[sequence] != 0) {
      return
    }

    val startIndex = PacketProcessor.getSequenceIndex(sequence)
    val packetSize = PacketProcessor.getPacketSize(startIndex, length)
    val hasMore = PacketProcessor.hasMoreMessage(header)
    if (!hasMore) {
      if (getAtomicNoMoreFlag().compareAndSet(false, true)) {
        this.packetSize = packetSize
      }
    }

    /*BleLogger.log(
        "{\"ReadBuffer\":{\"EOP\": ${!hasMore}, \"sequence\":$sequence}}"
    )*/
    // Check buffer is null or less than the packet size
    getBuffer()?.let {
      val bufferLength = it.size
      if (bufferLength < packetSize) {
        wrapBuffer(bufferLength, packetSize, BYTES_256)
      }
    } ?: run {
      setBuffer(createBuffer(packetSize))
      sparseSequence.clear()
    }

    // Collect current packet
    try {
      System.arraycopy(data, 1, getBuffer(), startIndex, length - 1 /* skip header */)
      // Mark this sequence is read
      sparseSequence.put(sequence, sequence)
    } catch (e: NullPointerException) {
      e.printStackTrace()
    }
  }
}