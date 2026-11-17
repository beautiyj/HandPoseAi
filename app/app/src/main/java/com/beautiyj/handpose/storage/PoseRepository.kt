package com.beautiyj.handpose.storage

interface PoseRepository {
    fun savePose(
        name: String,
        gesture: String,
        confidence: Float,
        sourceType: PoseSourceType = PoseSourceType.LIVE,
        thumbnail: String? = null,
        metadata: String = "",
        landmarksJson: String? = null
    ): Boolean

    fun updatePose(
        id: Long,
        name: String? = null,
        gesture: String? = null,
        confidence: Float? = null,
        sourceType: PoseSourceType? = null,
        metadata: String? = null,
        thumbnail: String? = null,
        landmarksJson: String? = null
    ): Boolean

    fun updatePoseName(id: Long, name: String): Boolean
    fun updatePoseMetadata(id: Long, metadata: String): Boolean
    fun updatePoseLandmarks(id: Long, landmarksJson: String): Boolean

    fun deletePose(id: Long): Boolean
    fun loadPoses(
        query: String = "",
        sortMode: PoseSortMode = PoseSortMode.LATEST,
        filterMode: PoseFilterMode = PoseFilterMode.ALL
    ): List<SavedPoseRecord>

    fun clear()
    fun loadLatestPose(): SavedPoseRecord?

    companion object {
        const val MAX_SAVED_POSES = 100
    }
}
