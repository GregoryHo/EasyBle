package com.ns.greg.easyble

import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build.VERSION_CODES
import com.ns.greg.ble.BleServer
import com.ns.greg.ble.interfacies.BleServerObserver
import java.util.UUID
import kotlin.LazyThreadSafetyMode.NONE

@TargetApi(VERSION_CODES.LOLLIPOP)
/**
 * @author gregho
 * @since 2018/8/7
 */
class BleDemoServer(
  context: Context,
  multipleClients: Boolean
) : AdvertiseCallback(), BleServerObserver {

  private val bleServer: BleServer by lazy(NONE) {
    BleServer(context, multipleClients)
  }

  fun open() {
    bleServer.subscribe(this)
    bleServer.open()
    // Add your service here
    //bleServer.addServices()
    bleServer.startAdvertising(UUID.randomUUID(), this)
  }

  fun close() {
    bleServer.close()
    bleServer.unsubscribe()
  }

  override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
    // Advertising success
  }

  override fun onStartFailure(errorCode: Int) {
    // Advertising failure
  }

  override fun onConnected(client: BluetoothDevice) {
    // When the client is connected
  }

  override fun onDisconnected(client: BluetoothDevice) {
    // When ths client is disconnected
  }

  override fun onServiceAdded(
    service: BluetoothGattService?,
    e: Exception?
  ) {
    // When ths service is added
  }

  override fun onWriteRequest(
    device: BluetoothDevice,
    uuid: UUID,
    e: Exception?
  ): ByteArray? {
    // When client request to write
    return null
  }

  override fun onWriteFinished(
    device: BluetoothDevice,
    uuid: UUID,
    data: ByteArray
  ) {
    // When client write finished
  }

  override fun onReadRequest(
    device: BluetoothDevice,
    uuid: UUID,
    e: Exception?
  ) {
    // When client request to read
  }

  override fun onReadFinished(
    device: BluetoothDevice,
    uuid: UUID
  ) {
    // When client read finished
  }
}