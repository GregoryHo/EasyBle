package com.ns.greg.ble.internal

import com.ns.greg.ble.internal.buffer.BleBuffer

/**
 * @author gregho
 * @since 2018/5/31
 */
internal class PacketProcessor {

  companion object {

    private const val MORE_MESSAGE = 1

    /**
     * Gets more message flag in header
     *
     * header & 0x11111111 and right shift 7 bits to get value
     *
     * @param header the header
     * @return flag is pull up or not (1/0)
     */
    fun hasMoreMessage(header: Byte): Boolean {
      return (header.toInt() and 0xFF) shr 7 == MORE_MESSAGE
    }

    /**
     * Gets sequence of packet form header
     *
     * @param header the header
     * @return sequence of packet
     */
    fun getSequence(header: Byte): Int {
      return header.toInt() and 0x7F
    }

    /**
     * Gets start index of current sequence
     *
     * @param sequence sequence of packet
     * @return start index of sequence
     */
    fun getSequenceIndex(sequence: Int): Int {
      return (sequence - 1) * BleBuffer.MAXIMUM_PAYLOAD_SIZE
    }

    /**
     * Gets packet size of current sequence
     *
     * @param index start index
     * @param length length of packet
     * @return size of packet
     */
    fun getPacketSize(index: Int, length: Int): Int {
      return index + length - 1
    }
  }
}