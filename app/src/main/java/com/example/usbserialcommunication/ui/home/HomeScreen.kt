package com.example.usbserialcommunication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.usbserialcommunication.extensions.convertMillisToDateTime
import com.example.usbserialcommunication.model.ReceiveData


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        viewModel.searchConnectableUSBDevice(context)
    }

    Box(modifier.statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box() {
                LazyColumn {
                    item {
                        Text(text = "사용 가능한 기기 리스트")
                    }
                    if (uiState.deviceInfoList.isEmpty()) {
                        item {
                            Text(text = "연결 가능한 기기 없음")
                        }
                    }
                    items(uiState.deviceInfoList.size) {
                        val device = uiState.deviceInfoList[it]
                        Button(onClick = {
                            if (!device.isConnected) {
                                viewModel.connect(
                                    context = context,
                                    connectDevice = device
                                )
                            } else {
                                viewModel.disconnect(device)
                            }

                        }) {
                            Text("${device.device.productName} connect : ${device.isConnected}")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(Color.Black)
            )
            StateMessage(uiState.message)
        }
    }

}

@Composable
fun StateMessage(
    message: List<ReceiveData>
) {
    LazyColumn {
        items(message.size) {
            Box(modifier = Modifier) {
                if (message.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "메시지 없음"
                    )
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray)
                ) {
                    Text(text = convertMillisToDateTime(message[it].time))
                    Text(text = "byte : ${message[it].size}")
                    Text(text = message[it].data)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

