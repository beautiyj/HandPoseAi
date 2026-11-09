package com.beautiyj.handpose.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.beautiyj.data.mediapipe.HandLandmarkerHelper
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.handpose.viewmodel.MainViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 카메라 화면 출력 & 제스처 표시 ui

// CameraPreviewScreen 함수 : 카메라 미리보기 및 연결, 제스처 텍스트 표시
// ProcessCameraProvider.getInstance(context) : 카메라 기기 준비
// PreviewView : 카메라 미리보기 화면 생성
// setSurfaceProvider(previewView.surfaceProvider) : 카메라 프레임 화면에 연결
// bindToLifecycle : 카메라 생명주기 연결

@Composable
fun CameraPreviewScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(
            context = context,
            listener = object : HandLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String, errorCode: Int) {
                    Log.e("CameraPreview", "Hand detection error: $error")
                }

                override fun onResults(result: List<HandPoseData>) {
                    viewModel.analyzeCameraFrame(result)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerHelper.clearHandLandmarker()
            analysisExecutor.shutdown()
        }
    }

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

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                handLandmarkerHelper.detectLiveStream(bitmap)
                                imageProxy.close()
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        HandPose3DView(
            poseData = viewModel.currentHandPose,
            gestureName = viewModel.uiState.gestureName,
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = "3D Pose Preview: ${viewModel.uiState.gestureName}\n${viewModel.uiState.description}",
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private fun ImageProxy.toBitmap(): Bitmap {
    val image = this.image ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(
        nv21,
        android.graphics.ImageFormat.NV21,
        width,
        height,
        null
    )

    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    val matrix = Matrix()
    matrix.postRotate(90f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
