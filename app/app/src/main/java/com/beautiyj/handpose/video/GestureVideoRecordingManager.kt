package com.beautiyj.handpose.video

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

class GestureVideoRecordingManager(
    private val context: Context
) {
    private var recording: Recording? = null
    private var stopHandler: Handler? = null

    fun start(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onStatus: (String) -> Unit,
        onUriSaved: (android.net.Uri) -> Unit,
        onError: (String) -> Unit,
        maxDurationSeconds: Int = 10
    ) {
        if (recording != null) return

        ProcessCameraProvider.getInstance(context).addListener({
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,
                    "HandPose_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HandPoseAI")
            }
            val outputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(values).build()

            val executor: Executor = ContextCompat.getMainExecutor(context)
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )

                recording = videoCapture.output
                    .prepareRecording(context, outputOptions)
                    .start(executor) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> onStatus("녹화 중 · 최대 ${maxDurationSeconds}초")
                            is VideoRecordEvent.Finalize -> {
                                recording = null
                                stopHandler?.removeCallbacksAndMessages(null)
                                stopHandler = null
                                if (event.hasError()) {
                                    onError("영상 녹화 실패: ${event.error}")
                                } else {
                                    onStatus("녹화 완료")
                                    event.outputResults.outputUri?.let(onUriSaved)
                                }
                            }
                            else -> Unit
                        }
                    }

                stopHandler = Handler(Looper.getMainLooper())
                stopHandler?.postDelayed({ stop() }, maxDurationSeconds * 1000L)
            } catch (error: Exception) {
                recording = null
                onError("영상 녹화 시작 실패: ${error.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        recording?.stop()
    }

    fun isRecording(): Boolean = recording != null
}
