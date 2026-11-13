package com.beautiyj.handpose.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.beautiyj.domain.usecase.AnalyzeHandUseCase
import com.beautiyj.handpose.analysis.GestureFlowManager
import com.beautiyj.handpose.storage.PoseFilterMode
import com.beautiyj.handpose.storage.PoseSortMode
import com.beautiyj.handpose.storage.PoseSourceType
import com.beautiyj.handpose.storage.PoseStorage
import com.beautiyj.handpose.storage.SavedPoseRecord

class MainViewModel(
    context: Context,
    private val analyzeHandUseCase: AnalyzeHandUseCase = AnalyzeHandUseCase()
) {
    private val poseStorage = PoseStorage(context)
    private val gestureFlowManager = GestureFlowManager()

    var uiState by mutableStateOf(HandUiState())
        private set

    var customPoseName by mutableStateOf("")
        private set

    var savedPoses by mutableStateOf<List<SavedPoseRecord>>(emptyList())
        private set

    var currentHandPose by mutableStateOf<HandPoseData?>(null)
        private set

    var poseQuery by mutableStateOf("")
        private set

    var poseSortMode by mutableStateOf(PoseSortMode.LATEST)
        private set

    var poseFilterMode by mutableStateOf(PoseFilterMode.ALL)
        private set

    var sourceType by mutableStateOf(PoseSourceType.LIVE)
        private set

    init {
        refreshSavedPoses()
    }

    fun updateCustomPoseName(name: String) {
        customPoseName = name
    }

    fun updatePoseQuery(query: String) {
        poseQuery = query
        refreshSavedPoses()
    }

    fun updatePoseSortMode(sortMode: PoseSortMode) {
        poseSortMode = sortMode
        refreshSavedPoses()
    }

    fun updatePoseFilterMode(filterMode: PoseFilterMode) {
        poseFilterMode = filterMode
        refreshSavedPoses()
    }

    fun refreshSavedPoses() {
        val records = poseStorage.loadPoses(
            query = poseQuery,
            sortMode = poseSortMode,
            filterMode = poseFilterMode
        )
        savedPoses = gestureFlowManager.sortSavedPoses(
            gestureFlowManager.filterSavedPoses(records, poseQuery, poseFilterMode),
            poseSortMode
        )
    }

    fun analyzeCameraFrame(handPoses: List<HandPoseData>) {
        sourceType = PoseSourceType.LIVE
        val result = gestureFlowManager.buildLiveDetectionResult(handPoses, analyzeHandUseCase)
        if (!gestureFlowManager.validateLiveFrame(handPoses)) {
            uiState = uiState.copy(
                gestureName = result.gestureName,
                confidence = result.confidence,
                description = result.description,
                isProcessing = false
            )
            currentHandPose = null
            return
        }

        currentHandPose = handPoses.first()
        uiState = uiState.copy(
            gestureName = result.gestureName,
            confidence = result.confidence,
            description = result.description,
            isProcessing = false
        )
    }

    fun analyzePhoto(handPoses: List<HandPoseData>) {
        sourceType = PoseSourceType.PHOTO
        val result = gestureFlowManager.buildPhotoDetectionResult(handPoses, analyzeHandUseCase)
        if (!gestureFlowManager.validatePhotoFrame(handPoses)) {
            uiState = uiState.copy(
                gestureName = result.gestureName,
                confidence = result.confidence,
                description = result.description,
                isProcessing = false
            )
            return
        }

        currentHandPose = handPoses.first()
        uiState = uiState.copy(
            gestureName = result.gestureName,
            confidence = result.confidence,
            description = result.description,
            isProcessing = false
        )
    }

    fun saveCurrentPose(thumbnail: String? = null, metadata: String = ""): Boolean {
        val activeGesture = uiState.gestureName
        if (!gestureFlowManager.canSavePose(activeGesture)) {
            return false
        }

        val payload = gestureFlowManager.buildPosePayload(
            name = customPoseName,
            gestureName = activeGesture,
            confidence = uiState.confidence,
            sourceType = sourceType,
            metadata = metadata,
            thumbnail = thumbnail,
            poseData = currentHandPose
        )

        val saved = poseStorage.savePose(
            name = gestureFlowManager.ensureSafeName(payload.finalName, savedPoses.size + 1),
            gesture = payload.gestureName,
            confidence = payload.confidence,
            sourceType = payload.sourceType,
            thumbnail = payload.thumbnail,
            metadata = payload.metadata,
            landmarksJson = payload.poseData?.let { serializeLandmarks(it) }
        )
        if (saved) {
            customPoseName = ""
            refreshSavedPoses()
        }
        return saved
    }

    fun deletePose(id: Long): Boolean {
        val ok = poseStorage.deletePose(id)
        if (ok) refreshSavedPoses()
        return ok
    }

    fun updatePoseName(id: Long, name: String): Boolean {
        val ok = poseStorage.updatePoseName(id, name)
        if (ok) refreshSavedPoses()
        return ok
    }

    fun updatePoseMetadata(id: Long, metadata: String): Boolean {
        val ok = poseStorage.updatePoseMetadata(id, metadata)
        if (ok) refreshSavedPoses()
        return ok
    }

    fun updatePoseLandmarks(id: Long, landmarksJson: String): Boolean {
        val ok = poseStorage.updatePoseLandmarks(id, landmarksJson)
        if (ok) refreshSavedPoses()
        return ok
    }

    fun clearSavedPoses() {
        poseStorage.clear()
        refreshSavedPoses()
    }

    fun reset() {
        uiState = HandUiState()
        currentHandPose = null
    }

    private fun serializeLandmarks(handPose: HandPoseData): String {
        return handPose.landmarks.joinToString("|") { landmark ->
            "${landmark.x},${landmark.y},${landmark.z}"
        }
    }
}

data class HandUiState(
    val gestureName: String = "대기 중",
    val confidence: Float = 0f,
    val description: String = "손 인식이 아직 준비되지 않았습니다.",
    val isProcessing: Boolean = false
)
