package com.beautiyj.domain.usecase

import com.beautiyj.domain.model.HandPoseData


// 손 인식 핵심 비즈니스 로직 AnalyzeHandUseCase.kt

// 손 인식
class AnalyzeHandUseCase {
    // 카메라 프레임에서 손 좌표 리스트 받아서 -> 손 동작 판단
    // 입력값은 손 좌표의 배열, 반환값은 제스처 명 & 신뢰도 & 설명
    fun analyzeLiveFrame(handPoses: List<HandPoseData>): HandGestureResult {
        if (handPoses.isEmpty()) {
            return HandGestureResult(
                gestureName = "손 인식 없음",
                confidence = 0f,
                description = "카메라에서 손을 인식하지 못했습니다. 손이 프레임 안에 들어와 있는지 확인하세요."
            )
        }

        return classifyGesture(handPoses.first())
    }

    // 사진 분석용 손 인식
    // 실시간 카메라와 동일한 로직으로 재사용
    fun analyzePhoto(handPoses: List<HandPoseData>): HandGestureResult {
        return analyzeLiveFrame(handPoses)
    }

    // 실제 동작(제스처) 판별 로직
    // 손 랜드마크 21개 포인트 기준으로 구분, 단순 좌표 비교 추정
    private fun classifyGesture(handPose: HandPoseData): HandGestureResult {
        val landmarks = handPose.landmarks
        if (landmarks.size < 21) {
            return HandGestureResult(
                gestureName = "감지 불가",
                confidence = 0.1f,
                description = "기본 랜드마크 수가 부족합니다."
            )
        }

        val wrist = landmarks[0]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]
        val pinkyTip = landmarks[20]

        val openPalm = listOf(indexTip, middleTip, ringTip, pinkyTip).count { it.y < wrist.y } >= 3
        val allClosed = listOf(indexTip, middleTip, ringTip, pinkyTip).count { it.y > wrist.y } >= 3

        return when {
            openPalm -> HandGestureResult(
                gestureName = "Open Palm",
                confidence = 0.92f,
                description = "손이 편 상태로 인식되었습니다."
            )
            allClosed -> HandGestureResult(
                gestureName = "Fist",
                confidence = 0.9f,
                description = "주먹 형태로 인식되었습니다."
            )
            else -> HandGestureResult(
                gestureName = "Unknown",
                confidence = 0.62f,
                description = "손 형태를 확실히 판별할 수 없습니다."
            )
        }
    }
}

// 손 동작 결과 모델 (데이터 클래스) ui, viewmodel에 전달용도
data class HandGestureResult(
    val gestureName: String,
    val confidence: Float,
    val description: String
)
