package com.example.usbserialcommunication.model

data class ReceiveData(
    val time: Long,
    val size: Int,
    val data: String
)