package com.example.usbserialcommunication.ui.home

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usbserialcommunication.extensions.log
import com.example.usbserialcommunication.model.DeviceInfo
import com.example.usbserialcommunication.model.ReceiveData
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.HexDump
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch







class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var usbPermission: UsbPermission = UsbPermission.Unknown

    init {
//        hasUsbPermission()
    }

    // 없어도 잘 동작함. 어떤 용도로 사용하는지 모르겠음.
    // 왜 브로드케스트를 사용?
    private fun hasUsbPermission() {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (INTENT_ACTION_GRANT_USB == intent!!.action) {
                    usbPermission =
                        if (intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED,
                                false,
                            )
                        ) {
                            UsbPermission.Granted
                        } else {
                            UsbPermission.Denied
                        }
                }
            }
        }
    }


    fun connect(
        context: Context,
        connectDevice: DeviceInfo,
        baudRate: Int = 19200,
        dataBits: Int = 8,
        stopBits: Int = 1,
        parity: Int = UsbSerialPort.PARITY_NONE
    ) {
        "connect call".log()
        viewModelScope.launch {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val usbConnection: UsbDeviceConnection? = usbManager.openDevice(connectDevice.device)
            "usbPermission : $usbPermission".log()
            if (usbConnection == null
                && usbPermission == UsbPermission.Unknown
                && !usbManager.hasPermission(connectDevice.device)
            ) {
                usbPermission = UsbPermission.Requested
                val flags =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
                val intent = Intent(INTENT_ACTION_GRANT_USB)
                intent.setPackage(context.packageName)
                val usbPermissionIntent: PendingIntent =
                    PendingIntent.getBroadcast(context, 0, intent, flags)
                usbManager.requestPermission(connectDevice.device, usbPermissionIntent)
                return@launch
            }

            try {
                val usbSerialPort = connectDevice.usbSerialPort
                usbSerialPort.open(usbConnection)
                try {
                    usbSerialPort.setParameters(baudRate, dataBits, stopBits, parity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (connectDevice.withIoManager) {
                    val usbIoManager = SerialInputOutputManager(
                        usbSerialPort,
                        object : SerialInputOutputManager.Listener {
                            override fun onNewData(data: ByteArray?) {
                                viewModelScope.launch {
                                    "receive data from ${connectDevice.device.productName} : $data"
                                    data?.let {
                                        receiveDataFromDevice(it)
                                    }
                                }
                            }

                            override fun onRunError(e: java.lang.Exception) {
                                viewModelScope.launch {
                                    e.printStackTrace()
                                    disconnect(connectedDevice = connectDevice)
                                }
                            }
                        })
                    usbIoManager.start()
                    updateUiState(action = {
                        copy(
                            deviceInfoList = deviceInfoList.map {
                                if (connectDevice.device == it.device) {
                                    it.copy(
                                        isConnected = true,
                                        usbIoManager = usbIoManager
                                    )
                                } else {
                                    it
                                }
                            })
                    })
                }

            } catch (e: Exception) {
                disconnect(connectedDevice = connectDevice)
            }
        }
    }


    private fun receiveDataFromDevice(data: ByteArray) {
        viewModelScope.launch {
            val message = ReceiveData(
                time = System.currentTimeMillis(),
                size = data.size,
                data = HexDump.dumpHexString(data) ?: "known data",
            )

            _uiState.emit(
                uiState.value.copy(
                    message = uiState.value.message + message
                )
            )
        }
    }

    fun disconnect(connectedDevice: DeviceInfo) {
        try {
            val usbIoManager = connectedDevice.usbIoManager
            if (usbIoManager != null) {
                usbIoManager.listener = null
                usbIoManager.stop()
            }

            connectedDevice.usbSerialPort.close()
            updateUiState(action = {
                copy(deviceInfoList = deviceInfoList.map {
                    if (connectedDevice.device == it.device) {
                        it.copy(
                            isConnected = false,
                            usbIoManager = null
                        )
                    } else {
                        it
                    }
                })
            })

            "disconnect success device : ${connectedDevice.device.productName}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun searchConnectableUSBDevice(context: Context) {
        viewModelScope.launch {
            val connectableDevices = mutableListOf<DeviceInfo>()
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val usbDefaultProber = UsbSerialProber.getDefaultProber()
            connectableDevices.clear()

            manager.deviceList.forEach {
                val driver = usbDefaultProber.probeDevice(it.value)
                if (driver != null) {
                    for (port in 0 until driver.ports.size) {
                        connectableDevices.add(
                            DeviceInfo(
                                device = it.value,
                                port = port,
                                driver = driver,
                                usbSerialPort = driver.ports[port]
                            )
                        )
                    }
                }
            }

            connectableDevices.forEach {
                "items: $it".log()
            }

            updateUiState {
                copy(
                    deviceInfoList = connectableDevices
                )
            }
        }

    }


    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }

    private fun updateUiState(action: HomeUiState.() -> HomeUiState) {
        _uiState.update { action(it) }
    }

    private fun CoroutineScope.launchWithCatching(
        action: suspend () -> Unit = {}
    ) {
        launch {
            try {
                action.invoke()
            } catch (e: Exception) {
//                error.emit(e)
            }
        }
    }

    companion object {
        private val INTENT_ACTION_GRANT_USB: String = "com.example.myapplication" + ".GRANT_USB"
    }
}

