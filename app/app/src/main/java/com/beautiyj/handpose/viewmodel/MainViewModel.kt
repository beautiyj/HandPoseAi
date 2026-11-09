package com.beautiyj.handpose.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.usecase.AnalyzeHandUseCase
import com.beautiyj.handpose.storage.PoseStorage
import com.beautiyj.handpose.storage.SavedPoseRecord

class MainViewModel(
    context: Context,
    private val analyzeHandUseCase: AnalyzeHandUseCase = AnalyzeHandUseCase()
) {
    private val poseStorage = PoseStorage(context)

    var uiState by mutableStateOf(HandUiState())
        private set

    var customPoseName by mutableStateOf("")
        private set

    var savedPoses by mutableStateOf<List<SavedPoseRecord>>(emptyList())
        private set

    var currentHandPose by mutableStateOf<HandPoseData?>(null)
        private set

    init {
        refreshSavedPoses()
    }

    fun updateCustomPoseName(name: String) {
        customPoseName = name
    }

    fun refreshSavedPoses() {
        savedPoses = poseStorage.loadPoses()
    }

    fun analyzeCameraFrame(handPoses: List<HandPoseData>) {
        if (handPoses.isEmpty()) {
            uiState = uiState.copy(
                gestureName = "손 인식 없음",
                confidence = 0f,
                description = "카메라에 손이 보이지 않습니다.",
                isProcessing = false
            )
            currentHandPose = null
            return
        }

        currentHandPose = handPoses.first()
        val result = analyzeHandUseCase.analyzeLiveFrame(handPoses)
        uiState = uiState.copy(
            gestureName = result.gestureName,
            confidence = result.confidence,
            description = result.description,
            isProcessing = false
        )
    }

    fun analyzePhoto(handPoses: List<HandPoseData>) {
        if (handPoses.isEmpty()) {
            uiState = uiState.copy(
                gestureName = "사진 인식 실패",
                confidence = 0f,
                description = "이미지에서 손을 찾지 못했습니다.",
                isProcessing = false
            )
            return
        }

        currentHandPose = handPoses.first()
        val result = analyzeHandUseCase.analyzePhoto(handPoses)
        uiState = uiState.copy(
            gestureName = result.gestureName,
            confidence = result.confidence,
            description = result.description,
            isProcessing = false
        )
    }

    fun saveCurrentPose() {
        val activeGesture = uiState.gestureName
        if (activeGesture.isBlank() || activeGesture == "대기 중" || activeGesture == "손 인식 없음" || activeGesture == "감지 불가") {
            return
        }

        val finalName = if (customPoseName.isBlank()) {
            "Pose_${savedPoses.size + 1}"
        } else {
            customPoseName.trim()
        }

        poseStorage.savePose(finalName, activeGesture, uiState.confidence)
        customPoseName = ""
        refreshSavedPoses()
    }

    fun clearSavedPoses() {
        poseStorage.clear()
        refreshSavedPoses()
    }

    fun reset() {
        uiState = HandUiState()
        currentHandPose = null
    }
}

data class HandUiState(
    val gestureName: String = "대기 중",
    val confidence: Float = 0f,
    val description: String = "손 인식이 아직 준비되지 않았습니다.",
    val isProcessing: Boolean = false
)
