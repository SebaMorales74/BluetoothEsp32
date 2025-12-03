package com.example.bluetoothesp32

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class DeviceInfoActivity : AppCompatActivity() {

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val deviceNameTextView = findViewById<TextView>(R.id.deviceNameTextView)
        val deviceAddressTextView = findViewById<TextView>(R.id.deviceAddressTextView)
        val deviceTypeTextView = findViewById<TextView>(R.id.deviceTypeTextView)
        val bondStateTextView = findViewById<TextView>(R.id.bondStateTextView)

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("btDevice", BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("btDevice")
        }

        if (device != null) {
            deviceNameTextView.text = device.name ?: "N/A"
            deviceAddressTextView.text = device.address
            deviceTypeTextView.text = when (device.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                else -> "Unknown"
            }
            bondStateTextView.text = when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> "Bonded"
                BluetoothDevice.BOND_BONDING -> "Bonding"
                else -> "None"
            }
        } else {
            deviceNameTextView.text = getString(R.string.no_device_found)
        }
    }
}