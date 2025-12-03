package com.example.bluetoothesp32.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    // IMPORTANTE PEDIR LOS PERMISOS.
    // Recuerda que en clases vimos que tenemos que respetar el consentimiento del usuario.
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {


            return null
        }
        return bluetoothAdapter?.bondedDevices
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, uuid: UUID): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            connectedDevice = device
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun sendData(data: String): Boolean {
        try {
            bluetoothSocket?.outputStream?.write(data.toByteArray())
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun receiveData(): String? {
        try {
            val inputStream = bluetoothSocket?.inputStream
            val available = inputStream?.available() ?: 0
            if (available > 0) {
                val bytes = ByteArray(available)
                inputStream?.read(bytes)
                return String(bytes)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            connectedDevice = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}