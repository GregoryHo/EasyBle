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
import com.ns.greg.ble.ConnectionState.FAILURE
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

  private val gattCallback: BleGattCallback by lazy(NONE) {
    BleGattCallback(this)
  }
  private lateinit var characteristicWrapper: BleCharacteristicWrapper
  @Volatile private var state = DISCONNECTED
  private var bluetoothGatt: BluetoothGatt? = null
  private var connectionObserver: BleConnectionObserver? = null

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
    when (state) {
      FAILURE, DISCONNECTED -> {
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

  override fun close() {
    setConnectionState(CLOSING)
    bluetoothGatt?.disconnect()
  }

  override fun read(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  ) {
    characteristicWrapper = BleCharacteristicWrapper(characteristic)
    readImp(characteristic, delayTime)
  }

  private fun readImp(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  ) {
    BleHook.instance.addTask(object : BaseRunnable<BaseRun>() {
      override fun runImp(): BaseRun? {
        bluetoothGatt?.readCharacteristic(characteristic)
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
    characteristicWrapper = BleCharacteristicWrapper(characteristic, data)
    writeImp(characteristic, delayTime)
  }

  private fun writeImp(
    characteristic: BluetoothGattCharacteristic,
    delayTime: Long
  ) {
    BleHook.instance.addTask(object : BaseRunnable<BaseRun>() {
      override fun runImp(): BaseRun? {
        characteristicWrapper.run {
          if (getCharacteristic() == characteristic) {
            characteristic.value = getWriteBuffer().write()
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            bluetoothGatt?.writeCharacteristic(characteristic)
          }
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
      if (this.state < CLOSING || state == CLOSED) {
        this.state = state
      }
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
      BleLogger.log("{OnConnectionStateChange:{\"status\": $status, \"newState\": $newState}}")
      when (status) {
        BluetoothGatt.GATT_SUCCESS -> {
          when (newState) {
            BluetoothProfile.STATE_CONNECTING -> {
              instance.setConnectionState(CONNECTING)
            }
            BluetoothProfile.STATE_CONNECTED -> {
              instance.setConnectionState(CONNECTED)
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
              instance.setConnectionState(DISCONNECTING)
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
              if (instance.getConnectionState() == CLOSING) {
                instance.setConnectionState(CLOSED)
              } else {
                instance.setConnectionState(DISCONNECTED)
              }
            }
          }
        }
        BluetoothGatt.GATT_FAILURE -> {
          instance.setConnectionState(FAILURE)
        }
      }

      instance.connectionObserver?.onConnectionStateChanged(instance.getConnectionState())
    }

    override fun onServicesDiscovered(
      gatt: BluetoothGatt?,
      status: Int
    ) {
      BleLogger.log("{OnDiscovered:{\"status\": $status}}")
      gatt?.let {
        instance.device.setServices(it.services)
        instance.connectionObserver?.onDiscovered(status)
      }
    }

    override fun onCharacteristicRead(
      gatt: BluetoothGatt?,
      characteristic: BluetoothGattCharacteristic?,
      status: Int
    ) {
      characteristic?.let {
        val uuid = it.uuid
        BleLogger.log("{Read:{\"uuid\": $uuid, \"status\": $status}}")
        when (status) {
          BluetoothGatt.GATT_SUCCESS -> {
            with(instance.characteristicWrapper.getReadBuffer()) {
              read(it.value)
              if (isAggregated()) {
                instance.connectionObserver?.onRead(it, getData())
                release(true)
              } else {
                instance.readImp(it, 0)
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
        BleLogger.log("{Write:{\"uuid\": $uuid, \"status\": $status}}")
        when (status) {
          BluetoothGatt.GATT_SUCCESS -> {
            with(instance.characteristicWrapper.getWriteBuffer()) {
              if (hasNext()) {
                instance.writeImp(characteristic, 0)
              } else {
                instance.connectionObserver?.onWrite(it, status)
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