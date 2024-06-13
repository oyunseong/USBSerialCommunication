package com.example.usbserialcommunication.ui.home

import com.example.usbserialcommunication.model.DeviceInfo
import com.example.usbserialcommunication.model.ReceiveData

data class HomeUiState(
    val deviceInfoList: List<DeviceInfo> = emptyList(),
    val message: List<ReceiveData> = emptyList(),
)