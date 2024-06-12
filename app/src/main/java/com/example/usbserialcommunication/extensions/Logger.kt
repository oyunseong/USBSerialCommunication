package com.example.usbserialcommunication.extensions

import android.os.Build
import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun String.log(tag: String = "fastLog") {
    Log.d(tag, this)
}


fun convertMillisToDateTime(millis: Long): String {
    // 밀리초 값을 Instant로 변환
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val instant = Instant.ofEpochMilli(millis)
        // Instant를 LocalDateTime으로 변환
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

        // 원하는 포맷으로 포맷터 설정
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // 포맷팅하여 문자열로 반환
        dateTime.format(formatter)
    } else {
        "Not supported"
        // 필요시 추가
    }
}