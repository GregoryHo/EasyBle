package com.ns.greg.ble.internal

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import com.ns.greg.ble.ConnectionState
import com.ns.greg.ble.interfacies.BleConnection
import com.ns.greg.ble.interfacies.BleDevice
import java.util.ArrayList
import java.util.UUID

/**
 * @author gregho
 * @since 2018/6/22
 */
internal class BleDeviceImp(
  private val context: Context,
  private val device: BluetoothDevice
) : BleDevice {

  private val services = ArrayList<BluetoothGattService>()
  private lateinit var connection: BleConnectionImp

  override fun getContext(): Context {
    return context
  }

  override fun getBluetoothDevice(): BluetoothDevice {
    return device
  }

  override fun createConnection(autoConnect: Boolean): BleConnection {
    connection = BleConnectionImp(context, this, autoConnect)
    return connection
  }

  override fun getConnectionState(): ConnectionState {
    return connection.getConnectionState()
  }

  override fun setServices(services: List<BluetoothGattService>) {
    this.services.clear()
    this.services.addAll(services)
  }

  override fun getServices(): List<BluetoothGattService> {
    return services
  }

  override fun getService(serviceUuid: UUID): BluetoothGattService? {
    synchronized(services) {
      val iterator = services.iterator()
      while (iterator.hasNext()) {
        val service = iterator.next()
        if (service.uuid == serviceUuid) {
          return service
        }
      }

      return null
    }
  }

  override fun getCharacteristics(serviceUuid: UUID): List<BluetoothGattCharacteristic>? {
    return getService(serviceUuid)?.characteristics
  }

  override fun getCharacteristic(
    serviceUuid: UUID,
    characteristicUuid: UUID
  ): BluetoothGattCharacteristic? {
    getService(
        serviceUuid
    )?.characteristics?.forEach {
      if (it.uuid == characteristicUuid) {
        return it
      }
    }

    return null
  }
}