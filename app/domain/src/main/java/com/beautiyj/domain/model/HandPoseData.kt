package com.beautiyj.domain.model

// 우리 앱에서 공통으로 사용할 손가락 좌표 모델
data class HandPoseData(
    val landmarks: List<Landmark>, // 21개의 점들
    val handedness: String,        // 왼손/오른손
    val score: Float               // 인식 정확도
)

data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float
)