package com.ns.greg.ble.services.characteristics

import android.bluetooth.BluetoothGattCharacteristic
import com.ns.greg.ble.internal.buffer.ReadBuffer
import com.ns.greg.ble.internal.buffer.WriteBuffer
import java.util.UUID

/**
 * @author gregho
 * @since 2018/7/20
 */
class BleCharacteristicWrapper internal constructor(
  val characteristic: BluetoothGattCharacteristic,
  data: ByteArray?
) {

  internal constructor(characteristic: BluetoothGattCharacteristic) : this(characteristic, null)

  private val readBuffer = ReadBuffer()
  private val writeBuffer = WriteBuffer()

  init {
    data?.run {
      writeBuffer.setData(this)
    }
  }

  fun setData(data: ByteArray) {
    with(writeBuffer) {
      release(false)
      setData(data)
    }
  }

  internal fun getReadBuffer(): ReadBuffer {
    return readBuffer
  }

  internal fun getWriteBuffer(): WriteBuffer {
    return writeBuffer
  }

  class Builder {

    private var uuid: UUID? = null
    private var properties = 0
    private var permissions = 0
    private var data: ByteArray? = null

    fun setUuid(uuid: UUID): Builder {
      this.uuid = uuid
      return this
    }

    fun addProperties(property: Int): Builder {
      properties = properties or property
      return this
    }

    fun addPermissions(permission: Int): Builder {
      permissions = permissions or permission
      return this
    }

    fun setData(data: ByteArray?): Builder {
      this.data = data
      return this
    }

    fun build(): BleCharacteristicWrapper {
      if (uuid == null) {
        throw NullPointerException("You didn't defined uuid for this service.")
      }

      if (properties == 0) {
        throw IllegalArgumentException("You didn't defined properties.")
      }

      if (permissions == 0) {
        throw IllegalArgumentException("You didn't defined permissions.")
      }

      if (data != null && (properties and 0x0F != BluetoothGattCharacteristic.PROPERTY_READ || permissions and 0x0F != BluetoothGattCharacteristic.PERMISSION_READ)) {
        throw IllegalArgumentException(
            "You can't assigned data to the characteristic which has no read properties and permission."
        )
      }

      return BleCharacteristicWrapper(
          BluetoothGattCharacteristic(uuid!!, properties, permissions), data
      )
    }
  }
}