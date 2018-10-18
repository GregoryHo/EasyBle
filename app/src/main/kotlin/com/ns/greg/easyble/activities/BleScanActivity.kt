package com.ns.greg.easyble.activities

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.ns.greg.ble.BleScanner
import com.ns.greg.ble.BtManager
import com.ns.greg.ble.interfacies.BleScanListener
import com.ns.greg.ble.interfacies.BluetoothListener
import com.ns.greg.easyble.Constants
import com.ns.greg.easyble.R
import com.ns.greg.easyble.utils.ViewBinder
import com.ns.greg.library.fastlightrecyclerview.base.BaseRecyclerViewHolder
import com.ns.greg.library.fastlightrecyclerview.base.SingleVHAdapter
import com.ns.greg.library.fastlightrecyclerview.listener.OnItemClickListener
import com.ns.greg.library.rt_permissionrequester.PermissionRequester.Builder
import com.ns.greg.library.rt_permissionrequester.external.RationaleOption
import com.ns.greg.library.rt_permissionrequester.external.RequestingPermission
import com.ns.greg.library.rt_permissionrequester.external.SimplePermissionListener
import java.lang.ref.WeakReference

/**
 * @author gregho
 * @since 2018/6/25
 */
class BleScanActivity : AppCompatActivity(), OnItemClickListener, OnClickListener,
    BleScanListener {

  companion object {

    const val SCAN_PERIOD = 15_000L
  }

  private val bleDeviceRv by ViewBinder.bind<RecyclerView>(this, R.id.ble_device_rv)
  private val scanButton by ViewBinder.bind<Button>(this, R.id.scan_btn)
  private var permissionRequested = false
  private var hasLocationPermission = false
  private val bleScanner = BleScanner(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!BtManager.isBluetoothSupported()) {
      Toast.makeText(applicationContext, "Device is not support bluetooth.", Toast.LENGTH_LONG)
          .show()
      finish()
    }

    setContentView(R.layout.activity_scan)
    bleDeviceRv.itemAnimator = null
    bleDeviceRv.setHasFixedSize(true)
    bleDeviceRv.layoutManager = LinearLayoutManager(applicationContext)
    bleDeviceRv.adapter = BleDeviceListAdapter(this, applicationContext, ArrayList())
    scanButton.setOnClickListener(this)
  }

  override fun onResume() {
    super.onResume()
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!locationManager.isProviderEnabled(
            LocationManager.GPS_PROVIDER
        ) && !locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    ) {
      Toast.makeText(applicationContext, "Please enable GPS.", Toast.LENGTH_LONG)
          .show()
    } else {
      if (BtManager.isBluetoothEnabled()) {
        if (!permissionRequested) {
          Builder(applicationContext).setPermissions(RequestingPermission.ACCESS_FINE_LOCATION)
              .setRationaleOption(
                  RationaleOption("Request", "Needs the location permission to scan nearby device.")
              )
              .setListener(object : SimplePermissionListener {
                override fun onGranted(permissions: MutableList<String>?) {
                  permissionRequested = true
                  permissions?.run {
                    for (permission in this) {
                      if (permission == RequestingPermission.ACCESS_FINE_LOCATION) {
                        hasLocationPermission = true
                        break
                      }
                    }
                  }
                }

                override fun onDenied(permissions: MutableList<String>?) {
                }
              })
              .build()
              .request()
        }
      } else {
        BtManager.enableBluetooth(applicationContext, object : BluetoothListener {
          override fun onAccepted() {
            Toast.makeText(applicationContext, "Bluetooth is enabled.", Toast.LENGTH_SHORT)
                .show()
          }

          override fun onDenied() {
            Toast.makeText(applicationContext, "Bluetooth is not enabled.", Toast.LENGTH_SHORT)
                .show()
            finish()
          }
        })
      }
    }
  }

  override fun onPause() {
    super.onPause()
    stopScan()
    clearScanList()
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.scan_btn -> {
        if (hasLocationPermission) {
          startScan()
        } else {
          Toast.makeText(
              applicationContext, "Please enable the location in app permissions.",
              Toast.LENGTH_LONG
          )
              .show()
        }
      }
    }
  }

  private fun startScan() {
    clearScanList()
    bleScanner.startScan(SCAN_PERIOD)
  }

  private fun stopScan() {
    bleScanner.stopScan()
  }

  private fun clearScanList() {
    getBleDeviceListAdapter().run {
      clearItems()
      notifyDataSetChanged()
    }
  }

  private fun getBleDeviceListAdapter(): BleDeviceListAdapter {
    return bleDeviceRv.adapter as BleDeviceListAdapter
  }

  override fun onRootViewClick(p0: Int) {
    val device = getBleDeviceListAdapter().getItem(p0)
    device?.let {
      // Using current intent to pass data
      val intent = Intent(this, CommonBleActivity::class.java)
      intent.putExtra(Constants.KEY_BLE_DEVICE, it)
      startActivity(intent)
    }
  }

  override fun onSpecificViewClick(
    p0: Int,
    p1: Int
  ) {
    // nothing to do
  }

  override fun onStartScan() {
    scanButton.text = "SCANNING"
  }

  override fun onScan(
    device: BluetoothDevice?,
    rssi: Int,
    scanRecord: ByteArray?
  ) {
    device?.run {
      val adapter = getBleDeviceListAdapter()
      val items = adapter.collection
      var added = false
      for (item in items) {
        if (item.address == address) {
          added = true
          break
        }
      }

      if (!added) {
        adapter.addItem(this)
        adapter.addRssi(rssi)
        runOnUiThread { adapter.notifyDataSetChanged() }
      }
    }
  }

  override fun onStopScan() {
    scanButton.text = "SCAN DEVICE"
  }

  /*--------------------------------
   * Ble device adapter
   *-------------------------------*/

  class BleDeviceListAdapter(
    reference: BleScanActivity,
    context: Context,
    list: ArrayList<BluetoothDevice>
  ) :
      SingleVHAdapter<BaseRecyclerViewHolder, BluetoothDevice>(context, list) {

    private val instance: BleScanActivity = WeakReference<BleScanActivity>(reference).get()!!
    private val rssis = ArrayList<Int>()

    override fun onCreateViewHolderImp(parent: ViewGroup?): BaseRecyclerViewHolder {
      return BaseRecyclerViewHolder(
          LayoutInflater.from(context).inflate(R.layout.item_ble_device_content, parent, false)
      ).also {
        it.onItemClickListener = instance
      }
    }

    override fun getInitItemCount(): Int {
      return 0
    }

    override fun onBindViewHolderImp(
      holder: BaseRecyclerViewHolder?,
      position: Int,
      listItem: BluetoothDevice?
    ) {
      listItem?.let {
        holder?.run {
          getTextView(R.id.name_tv).text = if (it.name == null) "Unknown" else it.name
          getTextView(R.id.address_tv).text = it.address
          getTextView(R.id.rssi_tv).text = getRssi(position).toString()
        }
      }
    }

    fun addRssi(rssi: Int) {
      rssis.add(rssi)
    }

    fun getRssi(position: Int): Int {
      return try {
        rssis[position]
      } catch (e: ArrayIndexOutOfBoundsException) {
        e.printStackTrace()
        0
      }
    }
  }
}