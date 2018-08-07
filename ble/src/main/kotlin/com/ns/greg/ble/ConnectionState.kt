package com.ns.greg.ble

/**
 * @author gregho
 * @since 2018/6/11
 */
enum class ConnectionState(private val state: Int) {

  FAILURE(-1),
  CONNECTING(0),
  CONNECTED(1),
  DISCONNECTING(2),
  DISCONNECTED(3),
  CLOSING(4),
  CLOSED(5);

  override fun toString(): String {
    return when (state) {
      -1 -> "FAILURE"
      0 -> "CONNECTING"
      1 -> "CONNECTED"
      2 -> "DISCONNECTING"
      3 -> "DISCONNECTED"
      4 -> "CLOSING"
      5 -> "CLOSE"
      else -> "UNKNOWN"
    }
  }
}