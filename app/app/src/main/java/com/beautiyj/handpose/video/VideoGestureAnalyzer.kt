package com.beautiyj.handpose.video

import android.content.Context
import android.net.Uri
import com.beautiyj.data.mediapipe.HandLandmarkerHelper
import com.beautiyj.domain.usecase.AnalyzeHandUseCase

class VideoGestureAnalyzer(context: Context) {
    private val extractor = VideoFrameExtractor(context)
    private val useCase = AnalyzeHandUseCase()

    fun analyze(
        uri: Uri,
        landmarker: HandLandmarkerHelper,
        onProgress: (Int) -> Unit = {}
    ): VideoGestureAnalysisResult {
        val analyzedFrames = mutableListOf<VideoGestureFrame>()
        var processedCount = 0
        val expectedFrameCount = 50
        var durationMs = 0L

        extractor.forEachFrame(uri) { timestampMs, bitmap ->
            durationMs = timestampMs
            val poses = landmarker.detectVideoFrame(bitmap, timestampMs)
            val pose = poses.firstOrNull()
            val result = useCase.analyzeLiveFrame(poses)

            analyzedFrames.add(
                VideoGestureFrame(
                    timestampMs = timestampMs,
                    poseData = pose,
                    gestureName = result.gestureName,
                    confidence = result.confidence
                )
            )
            processedCount++
            onProgress(((processedCount * 100) / expectedFrameCount).coerceAtMost(100))
        }

        val validFrames = analyzedFrames.filter { it.poseData != null }
        val dominant = validFrames.groupBy { it.gestureName }
            .maxByOrNull { it.value.size }?.key ?: "손 인식 없음"
        val average = validFrames.map { it.confidence }.average().toFloat()

        return VideoGestureAnalysisResult(
            frames = analyzedFrames,
            durationMs = durationMs,
            dominantGesture = dominant,
            averageConfidence = if (validFrames.isEmpty()) 0f else average
        )
    }
}
