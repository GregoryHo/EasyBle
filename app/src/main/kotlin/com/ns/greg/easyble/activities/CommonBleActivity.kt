package com.ns.greg.easyble.activities

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ns.greg.ble.BleClient
import com.ns.greg.ble.ConnectionState
import com.ns.greg.ble.ConnectionState.CONNECTED
import com.ns.greg.ble.ConnectionState.DISCONNECTED
import com.ns.greg.ble.ConnectionState.FAILURE
import com.ns.greg.ble.interfacies.BleConnection
import com.ns.greg.ble.interfacies.BleConnectionObserver

/**
 * @author gregho
 * @since 2018/6/25
 */
class CommonBleActivity : AppCompatActivity() {

  private lateinit var bleClient: BleClient
  private lateinit var connection: BleConnection

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val device = intent.getParcelableExtra<BluetoothDevice>("TEST")
    bleClient = BleClient(applicationContext, device)
  }

  override fun onResume() {
    super.onResume()
    val device = bleClient.getDevice()
    // create new connection
    connection = device.createConnection(false)
    // subscribe the connection
    connection.subscribe(object : BleConnectionObserver {
      override fun onConnectionStateChanged(state: ConnectionState) {
        when (state) {
          CONNECTED -> {
            // discover services
            connection.discoverServices()
          }
          FAILURE, DISCONNECTED -> {
            // re-open connection
          }
          else -> {
            // just ignored others
          }
        }
      }

      /**
       * Callback invoked when services is discovered.
       *
       * Get services by invoked [BleDevice.getServices]
       * Get specific service by invoked [BleDevice.getService]
       * Get characteristics by invoked [BleDevice.getCharacteristics]
       * Get specific characteristic by invoked [BleDevice.getCharacteristic]
       *
       * @param status if the discovered succeeds.
       */
      override fun onDiscovered(status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // connection setup complete
          // you can do some [READ/WRITE] action
          // or get service list form ble device
          val builder = StringBuilder()
          for (service in bleClient.getDevice().getServices()) {
            println("Service: [${service.uuid}]")
            for (characteristic in service.characteristics) {
              builder.append("[")
                  .append(characteristic.uuid)
                  .append("]")
            }

            println("Characteristics: $builder")
            builder.setLength(0)
          }

        } else {
          // discovered again
        }
      }

      override fun onRead(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray?
      ) {
        data?.run {
          // read succeeds.
        } ?: run {
          // read failure.
        }
      }

      override fun onWrite(
        characteristic: BluetoothGattCharacteristic,
        status: Int
      ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          // write succeeds.
        } else {
          // write failure.
        }
      }
    })
    // open the connection
    connection.open()
  }

  override fun onPause() {
    super.onPause()
    // unsubscribe connection
    connection.unsubscribe()
    // close connection
    connection.close()
  }
}