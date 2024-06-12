package com.example.usbserialcommunication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.usbserialcommunication.MainViewModel
import com.example.usbserialcommunication.ReceiveData


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext
    val connectableDevice by viewModel.items.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        viewModel.searchConnectableUSBDevice(context)
    }

    Column {
        Box(modifier = modifier.statusBarsPadding()) {
            LazyColumn {
                item {
                    Text(text = "사용 가능한 기기")
                }
                if(connectableDevice.isEmpty()){
                    item {
                        Text(text = "연결 가능한 기기 없음")
                    }
                }
                items(connectableDevice.size) {
                    Button(onClick = {
                        viewModel.connect(
                            context = context,
                            item = connectableDevice[it]
                        )
                    }) {
                        Text("${connectableDevice[it].device.deviceName} connect : ${uiState.isConnect}")
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
        Message(uiState.message)
    }
}

@Composable
fun Message(
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
                Column {
                    Text(text = message[it].time.toString())
                    Text(text = message[it].size.toString())
                    Text(text = message[it].data)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

