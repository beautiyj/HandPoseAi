package com.beautiyj.handpose.video

import com.beautiyj.domain.model.HandPoseData

data class VideoGestureFrame(
    val timestampMs: Long,
    val poseData: HandPoseData?,
    val gestureName: String,
    val confidence: Float
)

data class VideoGestureAnalysisResult(
    val frames: List<VideoGestureFrame>,
    val durationMs: Long,
    val dominantGesture: String,
    val averageConfidence: Float
)
