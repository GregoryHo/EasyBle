package com.ns.greg.ble.internal

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.ns.greg.ble.ConnectionState
import com.ns.greg.ble.ConnectionState.CLOSED
import com.ns.greg.ble.ConnectionState.CLOSING
import com.ns.greg.ble.ConnectionState.CONNECTED
import com.ns.greg.ble.ConnectionState.CONNECTING
import com.ns.greg.ble.ConnectionState.DISCONNECTED
import com.ns.greg.ble.ConnectionState.DISCONNECTING
import com.ns.greg.ble.interfacies.BleConnection
import com.ns.greg.ble.interfacies.BleConnectionObserver
import com.ns.greg.ble.interfacies.BleDevice
import com.ns.greg.ble.services.characteristics.BleCharacteristicWrapper
import com.ns.greg.library.fasthook.BaseRunnable
import com.ns.greg.library.fasthook.functions.BaseRun
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author gregho
 * @since 2018/6/22
 */
internal class BleConnectionImp(
  private val context: Context,
  private val device: BleDevice,
  private val autoConnect: Boolean
) : BleConnection {

  private companion object Constants {

    const val TAG = "BleConnection"
  }

  private val gattCallback: BleGattCallback by lazy(NONE) {
    BleGattCallback(this)
  }
  @Volatile private lateinit var characteristicWrapper: BleCharacteristicWrapper
  @Volatile private var state = DISCONNECTED
  private var bluetoothGatt: BluetoothGatt? = null
  private var connectionObserver: BleConnectionObserver? = null
  private var delayTime = 0L
  private var executedTime = 0L

  init {
    BleLogger.log(TAG, message = "create connection")
  }

  override fun subscribe(observer: BleConnectionObserver) {
    this.connectionObserver = observer
  }

  override fun unsubscribe() {
    this.connectionObserver = null
  }

  override fun discoverServices() {
    bluetoothGatt?.discoverServices()
  }

  override fun open() {
    BleLogger.log(TAG, "OPEN")
    when (getConnectionState()) {
      CONNECTED -> BleLogger.log(TAG, message = "device is already connected")
      DISCONNECTED, CLOSED -> {
        BleLogger.log(TAG, message = "connecting to the device")
        if (bluetoothGatt == null) {
          bluetoothGatt = device.getBluetoothDevice()
              .connectGatt(context, autoConnect, gattCallback)
        } else {
          bluetoothGatt?.connect()
        }
      }
      else -> {
        // nothing to do
      }
    }
  }

  override fun disconnect() {
    BleLogger.log(TAG, "DISCONNECT")
    when (getConnectionState()) {
      CONNECTING, CONNECTED -> {
        BleLogger.log(message = "disconnecting with the device")
        setConnectionState(DISCONNECTING)
        bluetoothGatt?.disconnect()
      }
      DISCONNECTING, DISCONNECTED -> BleLogger.log(
          message = "connect is already disconnecting/disconnected"
      )
      CLOSING, CLOSED -> BleLogger.log(message = "connection is already closing/closed")
    }
  }

  override fun close() {
    BleLogger.log(TAG, "CLOSE")
    when (getConnectionState()) {
      CONNECTING, CONNECTED -> {
        BleLogger.log(message = "closing with the device")
        setConnectionState(CLOSING)
        bluetoothGatt?.disconnect()
      }
      DISCONNECTING, DISCONNECTED -> gattClose()
      CLOSING, CLOSED -> BleLogger.log(message = "connection is already closing/closed")
    }
  }

  override fun read(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  ) {
    this.characteristicWrapper = BleCharacteristicWrapper(characteristic)
    this.delayTime = delayTime
    readImp(characteristic, delayTime)
  }

  private fun readImp(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  ) {
    BleLogger.log(TAG, "PREPARE OPERATION [READ]", "(${characteristic.uuid}), delay: $delayTime ms")
    BleHook.instance.addTask(object : BaseRunnable<BaseRun>() {
      override fun runImp(): BaseRun? {
        bluetoothGatt?.run {
          BleLogger.log(TAG, "READ CHARACTERISTIC", "(${characteristic.uuid})")
          readCharacteristic(characteristic)
          operationExecuted()
        }
        return null
      }
    })
        .addDelayTime(delayTime)
        .start()
  }

  override fun write(
    characteristic: BluetoothGattCharacteristic,
    data: ByteArray,
    delayTime: Long
  ) {
    this.characteristicWrapper = BleCharacteristicWrapper(characteristic, data)
    this.delayTime = delayTime
    writeImp(characteristic, delayTime)
  }

  private fun writeImp(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  ) {
    BleLogger.log(TAG, "PREPARE OPERATION [WRITE]", "(${characteristic.uuid}), delay: $delayTime ms")
    BleHook.instance.addTask(object : BaseRunnable<BaseRun>() {
      override fun runImp(): BaseRun? {
        characteristic.value = characteristicWrapper.getWriteBuffer()
            .write()
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        bluetoothGatt?.run {
          BleLogger.log(TAG, "WRITE CHARACTERISTIC", "(${characteristic.uuid})")
          writeCharacteristic(characteristic)
          operationExecuted()
        }

        return null
      }
    })
        .addDelayTime(delayTime)
        .start()
  }

  override fun notify(
    characteristic: BluetoothGattCharacteristic,
    enable: Boolean,
    delayTime: Long
  ) {
    BleHook.instance.addTask(object : BaseRunnable<BaseRun>() {
      override fun runImp(): BaseRun? {
        bluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        val descriptor =
          characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor.value =
          if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeCharacteristic(characteristic)
        return null
      }
    })
  }

  fun getConnectionState(): ConnectionState {
    synchronized(this) {
      return state
    }
  }

  fun setConnectionState(state: ConnectionState) {
    synchronized(this) {
      this.state = state
    }
  }

  private fun operationExecuted() {
    executedTime = System.currentTimeMillis()
  }

  private fun operationSpentTime() = System.currentTimeMillis() - executedTime

  private fun gattClose() {
    BleLogger.log(message = "close connection with the device")
    setConnectionState(CLOSED)
    bluetoothGatt = bluetoothGatt?.let {
      it.close()
      null
    }
  }

  class BleGattCallback(reference: BleConnectionImp) : BluetoothGattCallback() {

    private val instance: BleConnectionImp by lazy {
      WeakReference<BleConnectionImp>(reference).get()!!
    }

    override fun onConnectionStateChange(
      gatt: BluetoothGatt?,
      status: Int,
      newState: Int
    ) {
      BleLogger.log(TAG, "ON CONNECTION STATE CHANGE", "status: $status, newState: $newState")
      with(instance) {
        when (status) {
          BluetoothGatt.GATT_SUCCESS -> when (newState) {
            BluetoothProfile.STATE_CONNECTING -> setConnectionState(CONNECTING)
            BluetoothProfile.STATE_CONNECTED -> setConnectionState(CONNECTED)
            BluetoothProfile.STATE_DISCONNECTING -> setConnectionState(DISCONNECTING)
            BluetoothProfile.STATE_DISCONNECTED -> if (getConnectionState() == CLOSING) {
              gattClose()
            } else {
              setConnectionState(DISCONNECTED)
            }
          }
          else -> {
            when (newState) {
              BluetoothProfile.STATE_DISCONNECTED -> gattClose()
              else -> close()
            }
          }
        }

        connectionObserver?.onConnectionStateChanged(instance.getConnectionState())
      }
    }

    override fun onServicesDiscovered(
      gatt: BluetoothGatt?,
      status: Int
    ) {
      gatt?.let {
        BleLogger.log(TAG, "ON SERVICES DISCOVERED", "status: $status")
        if (status == BluetoothGatt.GATT_SUCCESS) {
          it.services?.forEach { service ->
            BleLogger.log(functionName = "PRIMARY SERVICE", message = "(${service.uuid})")
            BleLogger.log(functionName = "CHARACTERISTICS")
            service.characteristics?.forEach { characteristic ->
              BleLogger.log(message = "* (${characteristic.uuid})")
              BleLogger.log(message = "  properties: [${characteristic.properties}]")
            }
          }

          instance.device.setServices(it.services)
          instance.connectionObserver?.onDiscovered(status)
        } else {
          /* just disconnect with BLE device */
          instance.disconnect()
        }
      }
    }

    override fun onCharacteristicRead(
      gatt: BluetoothGatt?,
      characteristic: BluetoothGattCharacteristic?,
      status: Int
    ) {
      characteristic?.let {
        val uuid = it.uuid
        BleLogger.log(
            TAG, "ON CHARACTERISTIC READ",
            "($uuid), status: $status in ${instance.operationSpentTime()} ms"
        )
        when (status) {
          BluetoothGatt.GATT_SUCCESS -> {
            with(instance.characteristicWrapper.getReadBuffer()) {
              read(it.value)
              if (isAggregated()) {
                instance.connectionObserver?.onRead(it, getData())
                release(true)
              } else {
                instance.readImp(it, instance.delayTime)
              }
            }
          }
          else -> {
            instance.connectionObserver?.onRead(it, null)
          }
        }
      }
    }

    override fun onCharacteristicWrite(
      gatt: BluetoothGatt?,
      characteristic: BluetoothGattCharacteristic?,
      status: Int
    ) {
      characteristic?.let {
        val uuid = it.uuid
        BleLogger.log(
            TAG, "ON CHARACTERISTIC WRITE",
            "($uuid), status: $status in ${instance.operationSpentTime()} ms"
        )
        when (status) {
          BluetoothGatt.GATT_SUCCESS -> {
            with(instance.characteristicWrapper.getWriteBuffer()) {
              if (hasNext()) {
                instance.writeImp(characteristic, instance.delayTime)
              } else {
                instance.connectionObserver?.onWrite(it, status)
                release(true)
              }
            }
          }
          else -> {
            instance.connectionObserver?.onWrite(it, status)
          }
        }
      }
    }
  }
}