package com.beautiyj.handpose.ui

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.beautiyj.handpose.video.GestureVideoRecordingManager

@Composable
fun ShortVideoRecorderScreen(
    onFinished: (android.net.Uri) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf("녹화 준비") }
    var isRecording by remember { mutableStateOf(false) }
    val manager = remember { GestureVideoRecordingManager(context) }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        onDispose { manager.stop() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = status,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
        )

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                if (isRecording) {
                    manager.stop()
                } else {
                    manager.start(
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        onStatus = {
                            status = it
                            isRecording = it.startsWith("녹화 중")
                            if (it == "녹화 완료") isRecording = false
                        },
                        onUriSaved = onFinished,
                        onError = {
                            status = it
                            isRecording = false
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }) { Text(if (isRecording) "녹화 종료" else "10초 녹화") }

            Button(onClick = onClose) { Text("닫기") }
        }
    }
}
