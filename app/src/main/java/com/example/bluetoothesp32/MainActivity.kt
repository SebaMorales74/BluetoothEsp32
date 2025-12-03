package com.example.bluetoothesp32

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bluetoothesp32.bluetooth.BluetoothHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var connectButton: MaterialButton
    private lateinit var deviceInfoButton: MaterialButton
    private lateinit var dataTextView: TextView
    private lateinit var deviceNameEditText: TextInputEditText
    private lateinit var pairedDevicesAutoCompleteTextView: AutoCompleteTextView

    // UUID generica para el servicio SPP (Serial Port Profile)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothHelper = BluetoothHelper(this)
        connectButton = findViewById(R.id.connectButton)
        deviceInfoButton = findViewById(R.id.deviceInfoButton)
        dataTextView = findViewById(R.id.dataTextView)
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        pairedDevicesAutoCompleteTextView = findViewById(R.id.pairedDevicesAutoCompleteTextView)

        connectButton.setOnClickListener {
            handleBluetoothConnection()
        }

        deviceInfoButton.setOnClickListener {
            showDeviceInfo()
        }

        requestBluetoothPermissions()
        setupPairedDevicesMenu()
    }

    @SuppressLint("MissingPermission")
    private fun setupPairedDevicesMenu() {
        val pairedDevices = bluetoothHelper.getPairedDevices()
        val deviceNames = pairedDevices?.map { it.name }?.toMutableList() ?: mutableListOf()

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, deviceNames)
        pairedDevicesAutoCompleteTextView.setAdapter(adapter)

        pairedDevicesAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val deviceName = parent.adapter.getItem(position) as String
            deviceNameEditText.setText(deviceName)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleBluetoothConnection() {
        if (!bluetoothHelper.isBluetoothEnabled()) {
            Toast.makeText(this, getString(R.string.bt_disabled), Toast.LENGTH_SHORT).show()
            return
        }

        val deviceName = deviceNameEditText.text.toString()
        if (deviceName.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_device), Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothHelper.getPairedDevices()
        val device = pairedDevices?.find { it.name == deviceName }

        if (device != null) {
            if (bluetoothHelper.connectToDevice(device, sppUuid)) {
                Toast.makeText(this, "${getString(R.string.connected_to)} $deviceName", Toast.LENGTH_SHORT).show()
                startListeningForData()
                bluetoothHelper.sendData(getString(R.string.hello_android))
            } else {
                Toast.makeText(this, getString(R.string.conn_failed), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.not_paired), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceInfo() {
        val deviceName = deviceNameEditText.text.toString()
        if (deviceName.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_device), Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothHelper.getPairedDevices()
        val device = pairedDevices?.find { it.name == deviceName }

        if (device != null) {
            val intent = Intent(this, DeviceInfoActivity::class.java)
            intent.putExtra("btDevice", device)
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startListeningForData() {
        Thread {
            while (true) {
                val receivedData = bluetoothHelper.receiveData()
                if (receivedData != null) {
                    runOnUiThread {
                        dataTextView.text = receivedData
                    }
                    Log.d("BluetoothData", "Received: $receivedData")
                }
                Thread.sleep(100) // Small delay to avoid busy-waiting
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothHelper.disconnect()
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_BLUETOOTH_PERMISSIONS
        )
    }
}