package com.ns.greg.ble

import android.app.PendingIntent.getService
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.ParcelUuid
import android.support.annotation.RequiresApi
import com.ns.greg.ble.interfacies.BleServerObserver
import com.ns.greg.ble.internal.BleLogger
import com.ns.greg.ble.services.BleServiceOption
import com.ns.greg.ble.services.characteristics.BleCharacteristicWrapper
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.net.UnknownServiceException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author gregho
 * @since 2018/7/19
 */
class BleServer(
  private val context: Context,
  private val multipleClients: Boolean
) : BluetoothGattServerCallback() {

  private val bluetoothManager by lazy {
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  }
  private lateinit var gattServer: BluetoothGattServer
  private val atomicIsAdvertising = AtomicBoolean()
  private val bleServices = ArrayList<BleServiceOption>()
  private val characteristicWrappers = HashSet<BleCharacteristicWrapper>()
  private val clients = ArrayList<BluetoothDevice>()
  private val characteristicWrappersMap =
    HashMap<BluetoothDevice, ArrayList<BleCharacteristicWrapper>>()
  private var observer: BleServerObserver? = null

  fun getContext(): Context {
    return context
  }

  fun subscribe(bleServerObserver: BleServerObserver) {
    this.observer = bleServerObserver
  }

  fun unsubscribe() {
    this.observer = null
  }

  @Synchronized fun open() {
    BleLogger.log("{Open:{\"BLE server\": \"DOG CAGE\"}}")
    gattServer = bluetoothManager.openGattServer(context, this)
  }

  @Synchronized fun close() {
    gattServer.close()
  }

  @Synchronized fun addService(serviceOption: BleServiceOption) {
    bleServices.add(serviceOption)
    characteristicWrappers.addAll(serviceOption.getCharacteristicWrappers())
    gattServer.addService(serviceOption.getService())
  }

  fun addServices(vararg serviceOptions: BleServiceOption) {
    launch {
      serviceOptions.forEach {
        addService(it)
        delay(1000)
      }
    }
  }

  @Synchronized fun removeService(serviceUuid: UUID) {
    val services = bleServices
    var removed: BleServiceOption? = null
    val iterator = services.iterator()
    while (iterator.hasNext()) {
      val service = iterator.next()
      if (service.getService().uuid == serviceUuid) {
        removed = service
        break
      }
    }

    removed?.run {
      services.remove(this)
      characteristicWrappers.removeAll(getCharacteristicWrappers())
      gattServer.removeService(getService())
    }
  }

  fun removeServices(vararg serviceUuids: UUID) {
    serviceUuids.forEach {
      removeService(it)
    }
  }

  @RequiresApi(VERSION_CODES.LOLLIPOP)
  fun startAdvertising(
    uuid: UUID,
    callback: AdvertiseCallback
  ) {
    if (atomicIsAdvertising.compareAndSet(false, true)) {
      val settings = AdvertiseSettings.Builder()
          .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
          .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
          .setConnectable(true)
          .build()
      val advertiseData = AdvertiseData.Builder()
          .setIncludeDeviceName(true)
          .setIncludeTxPowerLevel(false)
          .addServiceUuid(ParcelUuid(uuid))
          .build()
      bluetoothManager.adapter.bluetoothLeAdvertiser.startAdvertising(
          settings, advertiseData, callback
      )
    }
  }

  @RequiresApi(VERSION_CODES.LOLLIPOP)
  fun startAdvertising(
    settings: AdvertiseSettings,
    advertiseData: AdvertiseData,
    period: Long,
    callback: AdvertiseCallback
  ) {
    if (atomicIsAdvertising.compareAndSet(false, true)) {
      bluetoothManager.adapter.bluetoothLeAdvertiser.startAdvertising(
          settings, advertiseData, callback
      )
    }
  }

  @RequiresApi(VERSION_CODES.LOLLIPOP)
  fun stopAdvertising(callback: AdvertiseCallback) {
    if (atomicIsAdvertising.compareAndSet(true, false)) {
      bluetoothManager.adapter.bluetoothLeAdvertiser.stopAdvertising(callback)
    }
  }

  private fun getCharacteristicWrapper(
    device: BluetoothDevice,
    uuid: UUID
  ): BleCharacteristicWrapper? {
    characteristicWrappersMap[device]?.forEach {
      if (it.getCharacteristic().uuid == uuid) {
        return it
      }
    }

    return null
  }

  @Synchronized fun updateCharacteristicWrapper(
    device: BluetoothDevice,
    uuid: UUID,
    data: ByteArray
  ) {
    for (key in characteristicWrappersMap.keys) {
      if (key == device) {
        characteristicWrappersMap[key]?.forEach {
          if (it.getCharacteristic().uuid == uuid) {
            it.setData(data)
            return@forEach
          }
        }
      }
    }
  }

  /**
   * Assigned the data to specific characteristic wrapper of each device.
   */
  @Synchronized fun updateCharacteristicWrappers(
    uuid: UUID,
    data: ByteArray
  ) {
    for (key in characteristicWrappersMap.keys) {
      characteristicWrappersMap[key]?.forEach {
        if (it.getCharacteristic().uuid == uuid) {
          it.setData(data)
        }
      }
    }
  }

  private fun addClient(device: BluetoothDevice) {
    synchronized(clients) {
      var isAdded = false
      val iterator = clients.iterator()
      while (iterator.hasNext()) {
        if (iterator.next() == device) {
          isAdded = true
          break
        }
      }

      if (!isAdded) {
        BleLogger.log("{Connected:{\"device\": \"$device\"}}")
        clients.add(device)
        characteristicWrappersMap[device] = ArrayList(characteristicWrappers)
        observer?.onConnected(device)
      }
    }
  }

  private fun removeClient(device: BluetoothDevice) {
    synchronized(clients) {
      var removed: BluetoothDevice? = null
      val iterator = clients.iterator()
      while (iterator.hasNext()) {
        val client = iterator.next()
        if (client == device) {
          removed = client
          break
        }
      }

      removed?.run {
        BleLogger.log("{Disconnected:{\"device\": \"$this\"}}")
        clients.remove(this)
        characteristicWrappersMap.remove(device)
        observer?.onDisconnected(this)
      }
    }
  }

  private fun isValidCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
    bleServices.forEach { s ->
      s.getService()
          .characteristics.forEach { c ->
        if (c.uuid == characteristic.uuid) {
          return true
        }
      }
    }

    return false
  }

  private fun hasWriteProperties(characteristic: BluetoothGattCharacteristic): Boolean {
    val properties = characteristic.properties and 0x0F
    return properties == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE || properties == BluetoothGattCharacteristic.PROPERTY_WRITE
  }

  private fun hasReadProperties(characteristic: BluetoothGattCharacteristic): Boolean {
    return characteristic.properties and 0x0F == BluetoothGattCharacteristic.PROPERTY_READ
  }

  override fun onConnectionStateChange(
    device: BluetoothDevice?,
    status: Int,
    newState: Int
  ) {
    device?.run {
      when (newState) {
        BluetoothProfile.STATE_CONNECTING -> {
          if (!multipleClients && !clients.isEmpty() && clients[0] != device) {
            // Only support one by one, cancel others connection
            gattServer.cancelConnection(this)
          }
        }
        BluetoothProfile.STATE_CONNECTED -> {
          if (multipleClients || clients.isEmpty()) {
            addClient(this)
          }
        }
        BluetoothProfile.STATE_DISCONNECTED -> {
          removeClient(this)
        }
      }
    }
  }

  override fun onMtuChanged(
    device: BluetoothDevice?,
    mtu: Int
  ) {
    // TODO: Should implement this.
  }

  override fun onServiceAdded(
    status: Int,
    service: BluetoothGattService?
  ) {
    service?.run {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        BleLogger.log("{ServiceAdded:{\"service\": ${this.uuid}}}")
        observer?.onServiceAdded(this, null)
      } else {
        observer?.onServiceAdded(
            null, UnknownServiceException("Unknown exception while adding service to GATT server.")
        )
      }
    }
  }

  override fun onCharacteristicWriteRequest(
    device: BluetoothDevice?,
    requestId: Int,
    characteristic: BluetoothGattCharacteristic?,
    preparedWrite: Boolean,
    responseNeeded: Boolean,
    offset: Int,
    value: ByteArray?
  ) {
    device?.let {
      characteristic?.also { c ->
        val uuid = c.uuid
        if (isValidCharacteristic(c) && hasWriteProperties(c)) {
          BleLogger.log("{WriteRequest:{\"characteristic\": $uuid}}")
          getCharacteristicWrapper(it, uuid)?.run {
            with(getReadBuffer()) {
              read(value)
              gattServer.sendResponse(
                  it, requestId, BluetoothGatt.GATT_SUCCESS, 0,
                  observer?.onWriteRequest(it, uuid, null)
              )
              if (isAggregated()) {
                observer?.onWriteFinished(it, uuid, getData())
                release(true)
              }
            }
          }
        } else {
          gattServer.sendResponse(it, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
          observer?.onWriteRequest(
              it, uuid, IllegalAccessException("No such characteristic to write.")
          )
        }
      }
    }
  }

  override fun onCharacteristicReadRequest(
    device: BluetoothDevice?,
    requestId: Int,
    offset: Int,
    characteristic: BluetoothGattCharacteristic?
  ) {
    device?.let {
      characteristic?.also { c ->
        val uuid = c.uuid
        if (isValidCharacteristic(c) && hasReadProperties(c)) {
          BleLogger.log("{ReadRequest:{\"characteristic\": $uuid}}")
          getCharacteristicWrapper(it, uuid)?.run {
            with(getWriteBuffer()) {
              observer?.onReadRequest(it, uuid, null)
              gattServer.sendResponse(
                  it, requestId, BluetoothGatt.GATT_SUCCESS, 0, write()
              )
              if (!hasNext()) {
                observer?.onReadFinished(it, uuid)
                release(false)
              }
            }
          }
        } else {
          gattServer.sendResponse(it, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
          observer?.onReadRequest(
              it, uuid, IllegalAccessException("No such characteristic to read.")
          )
        }
      }
    }
  }

  override fun onExecuteWrite(
    device: BluetoothDevice?,
    requestId: Int,
    execute: Boolean
  ) {
    // TODO: Should implement this.
  }

  override fun onNotificationSent(
    device: BluetoothDevice?,
    status: Int
  ) {
    // TODO: Should implement this.
  }
}