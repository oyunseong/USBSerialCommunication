package com.example.usbserialcommunication.model

import android.hardware.usb.UsbDevice
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager

data class DeviceInfo(
    val port: Int,
    val device: UsbDevice,
    val driver: UsbSerialDriver,
    val usbSerialPort: UsbSerialPort,
    val withIoManager: Boolean = true, // // read_modes[0]=event/io-manager, read_modes[1]=direct 필요시 추가
    val isConnected: Boolean = false,
    val usbIoManager: SerialInputOutputManager? = null
)