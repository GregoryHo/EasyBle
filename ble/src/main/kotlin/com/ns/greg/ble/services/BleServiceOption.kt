package com.ns.greg.ble.services

import android.bluetooth.BluetoothGattService
import com.ns.greg.ble.services.characteristics.BleCharacteristicWrapper
import java.util.UUID

/**
 * @author gregho
 * @since 2018/7/20
 */
class BleServiceOption private constructor(
  private val service: BluetoothGattService,
  private val characteristicWrappers: Array<out BleCharacteristicWrapper>
) {

  init {
    characteristicWrappers.forEach {
      service.addCharacteristic(it.getCharacteristic())
    }
  }

  fun getService(): BluetoothGattService {
    return service
  }

  fun getCharacteristicWrappers(): Array<out BleCharacteristicWrapper> {
    return characteristicWrappers
  }

  class Builder {

    private var service: BluetoothGattService? = null
    private var characteristics: Array<out BleCharacteristicWrapper>? = null

    fun setUuid(uuid: UUID): Builder {
      service = BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
      return this
    }

    fun setCharacteristics(vararg characteristics: BleCharacteristicWrapper): Builder {
      this.characteristics = characteristics
      return this
    }

    fun build(): BleServiceOption {
      if (service == null) {
        throw NullPointerException("You didn't defined uuid for this service.")
      }

      if (characteristics == null) {
        throw IllegalArgumentException("You didn't assign characteristic into service.")
      }

      return BleServiceOption(service!!, characteristics!!)
    }
  }
}