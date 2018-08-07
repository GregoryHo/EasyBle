package com.ns.greg.easyble.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ns.greg.easyble.R

/**
 * @author gregho
 * @since 2018/8/2
 */
class DemoActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_demo)
    findViewById<View>(R.id.server_btn).setOnClickListener {
      startActivity(Intent(this, BleServerActivity::class.java))
    }
    findViewById<View>(R.id.scan_btn).setOnClickListener {
      startActivity(Intent(this, BleScanActivity::class.java))
    }
  }
}