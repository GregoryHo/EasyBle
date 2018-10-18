package com.ns.greg.ble.internal.buffer

import com.ns.greg.ble.internal.BleLogger
import com.ns.greg.ble.internal.PacketProcessor

/**
 * @author gregho
 * @since 2018/5/31
 */
internal class WriteBuffer : BleBuffer {

  private var sequence = 0
  private var dataLength = 0

  constructor() : super()
  constructor(size: Int) : super(size)

  override fun release(cleanBuffer: Boolean) {
    sequence = 1
    setNoMoreFlag(false)
    if (cleanBuffer) {
      createBuffer(BYTES_256)
    }
  }

  fun setData(data: ByteArray?) {
    if (data == null) {
      throw IllegalArgumentException("Can't assigned null data.")
    }

    sequence = 1
    val bufferLength = getBuffer()?.size ?: run {
      setBuffer(createBuffer(BYTES_256))
      BYTES_256
    }

    dataLength = data.size
    if (dataLength / BleBuffer.MAXIMUM_PAYLOAD_SIZE > BleBuffer.MAXIMUM_SEQUENCE) {
      throw IllegalStateException(
          "The maximum sequence of assigned data exceeds ${BleBuffer.MAXIMUM_SEQUENCE}"
      )
    }

    if (bufferLength < dataLength) {
      wrapBuffer(bufferLength, dataLength, 0)
    }

    try {
      System.arraycopy(data, 0, getBuffer(), 0, dataLength)
    } catch (e: NullPointerException) {
      e.printStackTrace()
    }
  }

  fun write(): ByteArray? {
    getBuffer()?.let {
      val startIndex = PacketProcessor.getSequenceIndex(sequence)
      if (startIndex + MAXIMUM_PAYLOAD_SIZE >= dataLength) {
        setNoMoreFlag(true)
      }

      val payloadSize = Math.min(dataLength - startIndex, MAXIMUM_PAYLOAD_SIZE)
      val bytes = ByteArray(payloadSize + 1 /* plus one for header */)
      // Pull up sequence flag
      bytes[0] = ((sequence and 0x7F).toByte())
      if (!isFoundNoMoreFlag()) {
        // Pull up has more flag, 0x1000000
        bytes[0] = (bytes[0] + 0x80).toByte()
      }

      BleLogger.log(
          "write", "header: ${String.format(
          "%8s", Integer.toBinaryString(bytes[0].toInt() and 0xFF)
      ).replace(' ', '0')}"
      )
      BleLogger.log("", "EOP: ${isFoundNoMoreFlag()}, sequence: $sequence")
      System.arraycopy(it, startIndex, bytes, 1 /* skip header */, payloadSize)
      return bytes
    } ?: run {
      return null
    }
  }

  fun hasNext(): Boolean {
    if (!isFoundNoMoreFlag()) {
      sequence++
      return true
    }

    return false
  }
}