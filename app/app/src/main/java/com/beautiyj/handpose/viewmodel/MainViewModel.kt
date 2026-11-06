package com.beautiyj.handpose.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.usecase.AnalyzeHandUseCase

// 메인 뷰모델, ui 상태 관리 역할 (뷰모델은 분석 결과를 상태로 바꾸는 역할, 비즈니스 로직은 ui는 몰라도 됨)
// uiState: 화면에서 관리하는 상태 객체 - 제스처 명, 신뢰도, 설명, 처리 상태, 역할은 화면이 바뀌는 상태를 담는 용도

class MainViewModel(
    private val analyzeHandUseCase: AnalyzeHandUseCase = AnalyzeHandUseCase()
) {
    var uiState by mutableStateOf(HandUiState())
        private set

    // 카메라 프레임으로 손 데이터 얻으면 -> 결과를 uiState에 반영
    // 카메라 실시간 분석 결과를 화면 상태로 저장하는 용도
    fun analyzeCameraFrame(handPoses: List<HandPoseData>) {
        val result = analyzeHandUseCase.analyzeLiveFrame(handPoses)
        uiState = uiState.copy(
            gestureName = result.gestureName,
            confidence = result.confidence,
            description = result.description,
            isProcessing = false
        )
    }

    // 사진 인식용 분석 결과를 받으면 -> uiState에 반영
    // 사진 업로드 후 감지 결과를 화면에 넣는 용도
    fun analyzePhoto(handPoses: List<HandPoseData>) {
        val result = analyzeHandUseCase.analyzePhoto(handPoses)
        uiState = uiState.copy(
            gestureName = result.gestureName,
            confidence = result.confidence,
            description = result.description,
            isProcessing = false
        )
    }

    // ui 상태 초기화
    fun reset() {
        uiState = HandUiState()
    }
}

// 뷰모델 상태 담는 용도의 데이터 클래스
// ui가 어떤 제스처를 보여줘야하는지 반영하는 용도
data class HandUiState(
    val gestureName: String = "대기 중",
    val confidence: Float = 0f,
    val description: String = "손 인식이 아직 준비되지 않았습니다.",
    val isProcessing: Boolean = false
)
