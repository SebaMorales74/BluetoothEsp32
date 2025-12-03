package com.example.bluetoothesp32

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bluetoothesp32.bluetooth.BluetoothHelper
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var connectButton: Button
    private lateinit var dataTextView: TextView
    private lateinit var deviceNameEditText: EditText
    private lateinit var pairedDevicesSpinner: Spinner

    // Standard UUID for SPP (Serial Port Profile)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // You will need to handle the result of this request in your app
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
        dataTextView = findViewById(R.id.dataTextView)
        deviceNameEditText = findViewById(R.id.deviceNameEditText)
        pairedDevicesSpinner = findViewById(R.id.pairedDevicesSpinner)

        connectButton.setOnClickListener {
            handleBluetoothConnection()
        }

        requestBluetoothPermissions()
        setupPairedDevicesSpinner()
    }

    @SuppressLint("MissingPermission")
    private fun setupPairedDevicesSpinner() {
        val pairedDevices = bluetoothHelper.getPairedDevices()
        val deviceNames = pairedDevices?.map { it.name }?.toMutableList() ?: mutableListOf()
        deviceNames.add(0, getString(R.string.select_a_device))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pairedDevicesSpinner.adapter = adapter

        pairedDevicesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val deviceName = parent.getItemAtPosition(position) as String
                    deviceNameEditText.setText(deviceName)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleBluetoothConnection() {
        if (!bluetoothHelper.isBluetoothEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceName = deviceNameEditText.text.toString()
        if (deviceName.isEmpty() || deviceName == getString(R.string.select_a_device)) {
            Toast.makeText(this, "Please enter or select a device name", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothHelper.getPairedDevices()
        val esp32Device = pairedDevices?.find { it.name == deviceName }

        if (esp32Device != null) {
            if (bluetoothHelper.connectToDevice(esp32Device, sppUuid)) {
                Toast.makeText(this, "Connected to $deviceName", Toast.LENGTH_SHORT).show()
                startListeningForData()
                bluetoothHelper.sendData("Hello from Android!")
            } else {
                Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Device not paired", Toast.LENGTH_SHORT).show()
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