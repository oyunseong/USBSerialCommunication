package com.example.usbserialcommunication

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.extensions.log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.HexDump
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReceiveData(
    val time: Long,
    val size: Int,
    val data: String
)

data class Item(
    val device: UsbDevice,
    val driver: UsbSerialDriver,
    val port: Int
)

data class MainUiState(
    val deviceId: Int = 0,
    val portNum: Int = 0,
    val baudRate: Int = 0,
    val withIoManager: Boolean = false,
    val isConnect: Boolean = false,
    val usbSerialPort: UsbSerialPort? = null,
    val message: List<ReceiveData> = emptyList()
)

class MainViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private var usbPermission: UsbPermission = UsbPermission.Unknown
    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null

    init {
        hasUsbPermission()
    }

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
        item: Item
    ) {
        val baudRate = 19200
        "connect call".log()
        viewModelScope.launch {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            usbSerialPort = item.driver.ports[item.port]
            val usbConnection: UsbDeviceConnection? = usbManager.openDevice(item.device)
            if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(
                    item.device
                )
            ) {
                usbPermission = UsbPermission.Requested
                val flags =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
                val intent = Intent(INTENT_ACTION_GRANT_USB)
                intent.setPackage(context.packageName)
                val usbPermissionIntent: PendingIntent =
                    PendingIntent.getBroadcast(context, 0, intent, flags)
                usbManager.requestPermission(item.device, usbPermissionIntent)
                return@launch
            }
            if (usbConnection == null) {
                return@launch
            }
            try {
                usbSerialPort!!.open(usbConnection)
                try {
                    usbSerialPort!!.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val withIoManager = true
                if (withIoManager) {
                    usbIoManager = SerialInputOutputManager(
                        usbSerialPort,
                        object : SerialInputOutputManager.Listener {
                            override fun onNewData(data: ByteArray?) {
                                "data : $data".log()
                                viewModelScope.launch {
                                    data?.let {
                                        receiveDataFromDevice(it)
                                    }
                                }
                            }

                            override fun onRunError(e: java.lang.Exception?) {
                                viewModelScope.launch {
                                    disconnect()
                                }
                            }
                        })
                    usbIoManager!!.start()
                }
                _uiState.emit(
                    uiState.value.copy(
                        isConnect = true
                    )
                )
            } catch (e: Exception) {
                disconnect()
            }
        }
    }


    private fun receiveDataFromDevice(data: ByteArray) {
        viewModelScope.launch {
            val message = ReceiveData(
                time = System.currentTimeMillis(),
                size = data.size,
                data = HexDump.dumpHexString(data) ?: "",
            )

            _uiState.emit(
                uiState.value.copy(
                    message = uiState.value.message + message
                )
            )
        }
    }

    private fun disconnect() {
//        controlLines.stop();
//        if(usbIoManager != null) {
//            usbIoManager.setListener(null);
//            usbIoManager.stop();
//        }
//        usbIoManager = null;
        try {
            usbSerialPort?.close();
        } catch (e: Exception) {
            e.printStackTrace()
        }
        usbSerialPort = null;
    }

    val items: MutableStateFlow<List<Item>> = MutableStateFlow(emptyList())

    // TODO renaming
    fun searchConnectableUSBDevice(context: Context) {
        viewModelScope.launch {
            val connectableDevice = mutableListOf<Item>()
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val usbDefaultProber = UsbSerialProber.getDefaultProber()
//        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            connectableDevice.clear()

            manager.deviceList.forEach {
                val driver = usbDefaultProber.probeDevice(it.value)
                if (driver != null) {
                    for (port in 0 until driver.ports.size) {
                        connectableDevice.add(
                            Item(
                                device = it.value,
                                port = port,
                                driver = driver
                            )
                        )
                    }
                }
            }

            connectableDevice.forEach {
                "items: $it".log()
            }
            items.emit(connectableDevice)
        }

    }


    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
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

