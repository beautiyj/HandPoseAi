package com.beautiyj.handpose.ui

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.beautiyj.handpose.viewmodel.MainViewModel

// 카메라 화면 출력 & 제스처 표시 ui

// CameraPreviewScreen 함수 : 카메라 미리보기 및 연결, 제스처 텍스트 표시
// ProcessCameraProvider.getInstance(context) : 카메라 기기 준비
// PreviewView : 카메라 미리보기 화면 생성
// setSurfaceProvider(previewView.surfaceProvider) : 카메라 프레임 화면에 연결
// bindToLifecycle : 카메라 생명주기 연결

@Composable
fun CameraPreviewScreen(viewModel: MainViewModel = MainViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        Text(
            text = "Gesture: ${viewModel.uiState.gestureName}\n${viewModel.uiState.description}",
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}