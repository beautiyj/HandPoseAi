package com.beautiyj.data.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    val context: Context,
    val listener: LandmarkerListener? = null
) {
    private var handLandmarker: HandLandmarker? = null

    init { setupHandLandmarker() }

    fun setupHandLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task") // 아까 넣은 파일명
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM) // 실시간 모드
            .setResultListener(this::returnResult)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    // 카메라 화면(Bitmap)을 분석기에 넣는 함수
    fun detectLiveStream(bitmap: Bitmap) {
        val frameTime = SystemClock.uptimeMillis()
        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnResult(result: HandLandmarkerResult, input: Any) {
        val handPoseList = result.landmarks().mapIndexed { index, landmarks ->
            HandPoseData(
                landmarks = landmarks.map { Landmark(it.x(), it.y(), it.z()) },
                handedness = result.handedness()[index][0].categoryName(),
                score = result.handedness()[index][0].score()
            )
        }
        listener?.onResults(handPoseList)
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(result: List<HandPoseData>)
    }
}