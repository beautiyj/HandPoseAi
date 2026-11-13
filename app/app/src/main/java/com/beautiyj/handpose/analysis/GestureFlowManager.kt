package com.beautiyj.handpose.analysis

import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.usecase.AnalyzeHandUseCase
import com.beautiyj.domain.usecase.HandGestureResult
import com.beautiyj.handpose.storage.PoseFilterMode
import com.beautiyj.handpose.storage.PoseSortMode
import com.beautiyj.handpose.storage.PoseSourceType
import com.beautiyj.handpose.storage.SavedPoseRecord

class GestureFlowManager {
    fun validateLiveFrame(handPoses: List<HandPoseData>): Boolean {
        return handPoses.isNotEmpty() && handPoses.first().landmarks.size >= 21
    }

    fun validatePhotoFrame(handPoses: List<HandPoseData>): Boolean {
        return handPoses.isNotEmpty() && handPoses.first().landmarks.size >= 21
    }

    fun buildLiveDetectionResult(
        handPoses: List<HandPoseData>,
        useCase: AnalyzeHandUseCase
    ): HandGestureResult {
        return if (validateLiveFrame(handPoses)) {
            useCase.analyzeLiveFrame(handPoses)
        } else {
            HandGestureResult(
                gestureName = "손 인식 없음",
                confidence = 0f,
                description = "실시간 카메라에서 손이 인식되지 않았습니다. 프레임 내부에 손을 넣어 주세요."
            )
        }
    }

    fun buildPhotoDetectionResult(
        handPoses: List<HandPoseData>,
        useCase: AnalyzeHandUseCase
    ): HandGestureResult {
        return if (validatePhotoFrame(handPoses)) {
            useCase.analyzePhoto(handPoses)
        } else {
            HandGestureResult(
                gestureName = "사진 인식 실패",
                confidence = 0f,
                description = "이미지에서 손을 찾지 못했습니다. 배경을 줄이고 손을 선명하게 촬영해 주세요."
            )
        }
    }

    fun shouldShowGuideline(handPoses: List<HandPoseData>): Boolean {
        return handPoses.isEmpty() || handPoses.first().landmarks.size < 21
    }

    fun buildGuidelineMessage(sourceType: PoseSourceType): String {
        return when (sourceType) {
            PoseSourceType.LIVE -> "가이드라인 안에 손을 넣고, 손등이 아닌 손바닥이 보이게 유지해 주세요."
            PoseSourceType.PHOTO -> "사진에서 손이 선명하게 보이도록 고화질 이미지를 선택해 주세요. 배경을 단순하게 유지하면 인식률이 높습니다."
        }
    }

    fun buildPosePayload(
        name: String,
        gestureName: String,
        confidence: Float,
        sourceType: PoseSourceType,
        metadata: String,
        thumbnail: String?,
        poseData: HandPoseData?
    ): PosePayload {
        return PosePayload(
            finalName = if (name.isBlank()) "Pose_${System.currentTimeMillis()}" else name.trim(),
            gestureName = gestureName,
            confidence = confidence,
            sourceType = sourceType,
            metadata = metadata,
            thumbnail = thumbnail,
            poseData = poseData
        )
    }

    fun canSavePose(gestureName: String): Boolean {
        return gestureName.isNotBlank() &&
            gestureName != "대기 중" &&
            gestureName != "손 인식 없음" &&
            gestureName != "감지 불가" &&
            gestureName != "사진 인식 실패"
    }

    fun filterSavedPoses(
        records: List<SavedPoseRecord>,
        query: String,
        mode: PoseFilterMode
    ): List<SavedPoseRecord> {
        val normalized = query.trim()
        return records.filter { record ->
            val matchesQuery = normalized.isEmpty() ||
                record.name.contains(normalized, ignoreCase = true) ||
                record.gesture.contains(normalized, ignoreCase = true)
            val matchesMode = when (mode) {
                PoseFilterMode.ALL -> true
                PoseFilterMode.LIVE -> record.sourceType == PoseSourceType.LIVE.value
                PoseFilterMode.PHOTO -> record.sourceType == PoseSourceType.PHOTO.value
            }
            matchesQuery && matchesMode
        }
    }

    fun sortSavedPoses(
        records: List<SavedPoseRecord>,
        mode: PoseSortMode
    ): List<SavedPoseRecord> {
        return when (mode) {
            PoseSortMode.LATEST -> records.sortedByDescending { it.savedAt }
            PoseSortMode.OLDEST -> records.sortedBy { it.savedAt }
            PoseSortMode.NAME -> records.sortedBy { it.name.lowercase() }
        }
    }

    fun buildDetectionSummary(gestureName: String, confidence: Float): String {
        return "${gestureName} · ${(confidence * 100).toInt()}%"
    }

    fun ensureSafeName(name: String, fallbackIndex: Int): String {
        return if (name.isBlank()) {
            "사용자 포즈 $fallbackIndex"
        } else {
            name.trim()
        }
    }

    fun buildPhotoSourceHint(): String {
        return "권장: 손이 선명하게 보이는 고화질 사진, 얼굴 배경은 제외, 손은 프레임 중앙에 위치"
    }
}

data class PosePayload(
    val finalName: String,
    val gestureName: String,
    val confidence: Float,
    val sourceType: PoseSourceType,
    val metadata: String,
    val thumbnail: String?,
    val poseData: HandPoseData?
)
