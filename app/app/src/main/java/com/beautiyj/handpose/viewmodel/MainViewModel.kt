package com.beautiyj.handpose.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.beautiyj.domain.usecase.AnalyzeHandUseCase
import com.beautiyj.handpose.analysis.GestureFlowManager
import com.beautiyj.handpose.storage.PoseFilterMode
import com.beautiyj.handpose.storage.PoseRepository
import com.beautiyj.handpose.storage.PoseSortMode
import com.beautiyj.handpose.storage.PoseSourceType
import com.beautiyj.handpose.storage.PoseStorage
import com.beautiyj.handpose.storage.SavedPoseRecord
import com.beautiyj.handpose.video.GestureVideoCaptureManager
import com.beautiyj.handpose.video.VideoGestureAnalysisResult
import com.beautiyj.handpose.video.VideoGestureAnalyzer
import com.beautiyj.handpose.video.VideoFrameDiagnostics
import com.beautiyj.handpose.video.VideoAnalysisRepository
import com.beautiyj.handpose.video.VideoAnalysisRepositoryImpl
import java.util.concurrent.Executors

class MainViewModel(
    private val context: Context,
    private val poseRepository: PoseRepository = PoseStorage(context),
    private val gestureFlowManager: GestureFlowManager = GestureFlowManager(),
    private val gestureVideoCaptureManager: GestureVideoCaptureManager = GestureVideoCaptureManager(),
    private val analyzeHandUseCase: AnalyzeHandUseCase = AnalyzeHandUseCase(),
    private val videoAnalysisRepository: VideoAnalysisRepository = VideoAnalysisRepositoryImpl(context)
) {
    private val poseStorage = poseRepository

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

    var videoCaptureStatus by mutableStateOf("대기 중")
        private set

    var videoCaptureActive by mutableStateOf(false)
        private set

    var sampledVideoFrameCount by mutableStateOf(0)
        private set

    var videoAnalysisProgress by mutableStateOf(0)
        private set

    var videoAnalysisResult by mutableStateOf<VideoGestureAnalysisResult?>(null)
        private set

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    private val videoExecutor = Executors.newSingleThreadExecutor()
    private val videoFrameDiagnostics = VideoFrameDiagnostics()

    var videoDiagnosticLabel by mutableStateOf("진단 대기")
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
        recordVideoSampleIfNeeded(currentHandPose)
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

    fun startVideoClipCapture(durationSeconds: Int = 10) {
        gestureVideoCaptureManager.beginClip()
        videoCaptureActive = true
        sampledVideoFrameCount = 0
        videoCaptureStatus = "${durationSeconds}초 영상 클립 수집 시작"
    }

    fun completeVideoClipCapture(durationSeconds: Int = 10) {
        val clip = gestureVideoCaptureManager.endClip()
        val summary = gestureVideoCaptureManager.summarizeRecordedSamples()
        videoCaptureActive = false
        sampledVideoFrameCount = clip.sampleCount
        videoCaptureStatus = "${durationSeconds}초 영상 클립 처리 완료 · ${summary.dominantGesture} (${(summary.averageConfidence * 100).toInt()}%)"
    }

    fun recordVideoSampleIfNeeded(handPose: HandPoseData?) {
        if (!videoCaptureActive || handPose == null) return
        val result = analyzeHandUseCase.analyzeLiveFrame(listOf(handPose))
        gestureVideoCaptureManager.recordSample(
            gestureName = result.gestureName,
            confidence = result.confidence,
            timestampMs = System.currentTimeMillis()
        )
        sampledVideoFrameCount = gestureVideoCaptureManager.recordedSamples().size
    }

    fun analyzeUploadedVideo(uri: Uri) {
        selectedVideoUri = uri
        videoAnalysisProgress = 0
        videoAnalysisResult = null
        videoCaptureStatus = "영상 프레임 분석 중"

        videoExecutor.execute {
            try {
                val result = videoAnalysisRepository.analyze(
                    uri = uri,
                    onProgress = { progress ->
                        Handler(Looper.getMainLooper()).post {
                            videoAnalysisProgress = progress
                        }
                    }
                )
                Handler(Looper.getMainLooper()).post {
                    videoAnalysisResult = result
                    sampledVideoFrameCount = result.frames.size
                    repeat(result.frames.size.coerceAtMost(50)) { videoFrameDiagnostics.recordFrame() }
                    videoDiagnosticLabel = videoFrameDiagnostics.snapshot(640, 360).displayLabel()
                    videoCaptureStatus = "영상 분석 완료 · ${result.dominantGesture} (${(result.averageConfidence * 100).toInt()}%)"
                }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post {
                    videoCaptureStatus = "영상 분석 실패"
                }
            }
        }
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
