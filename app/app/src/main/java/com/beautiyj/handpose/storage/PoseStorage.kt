package com.beautiyj.handpose.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Date

enum class PoseSourceType(val value: String) {
    LIVE("LIVE"),
    PHOTO("PHOTO")
}

enum class PoseSortMode(val label: String) {
    LATEST("최신순"),
    OLDEST("오래된순"),
    NAME("제목순")
}

enum class PoseFilterMode(val label: String) {
    ALL("전체"),
    LIVE("실시간"),
    PHOTO("사진")
}

data class SavedPoseRecord(
    val id: Long = 0L,
    val name: String,
    val gesture: String,
    val confidence: Float,
    val savedAt: Long,
    val sourceType: String = PoseSourceType.LIVE.value,
    val thumbnail: String? = null,
    val metadata: String = "",
    val landmarksJson: String? = null
)

class PoseStorage(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPrefs: SharedPreferences = appContext.getSharedPreferences("hand_pose_store", Context.MODE_PRIVATE)
    private val dbHelper = PoseDbHelper(appContext)

    init {
        migrateLegacyDataIfNeeded()
    }

    fun savePose(
        name: String,
        gesture: String,
        confidence: Float,
        sourceType: PoseSourceType = PoseSourceType.LIVE,
        thumbnail: String? = null,
        metadata: String = "",
        landmarksJson: String? = null
    ): Boolean {
        val current = loadPoses()
        if (current.size >= MAX_SAVED_POSES) {
            val oldest = current.minByOrNull { it.savedAt }
            oldest?.let { deletePose(it.id) }
            if (loadPoses().size >= MAX_SAVED_POSES) {
                return false
            }
        }

        val trimmed = name.trim()
        val finalName = if (trimmed.isEmpty()) {
            "사용자 포즈 ${loadPoses().size + 1}"
        } else {
            trimmed
        }

        dbHelper.insertPose(
            name = finalName,
            gesture = gesture,
            confidence = confidence,
            savedAt = Date().time,
            sourceType = sourceType.value,
            thumbnail = thumbnail,
            metadata = metadata,
            landmarksJson = landmarksJson
        )
        return true
    }

    fun updatePose(
        id: Long,
        name: String? = null,
        gesture: String? = null,
        confidence: Float? = null,
        sourceType: PoseSourceType? = null,
        metadata: String? = null,
        thumbnail: String? = null,
        landmarksJson: String? = null
    ): Boolean {
        return dbHelper.updatePose(
            id = id,
            name = name,
            gesture = gesture,
            confidence = confidence,
            sourceType = sourceType?.value,
            metadata = metadata,
            thumbnail = thumbnail,
            landmarksJson = landmarksJson
        )
    }

    fun updatePoseName(id: Long, name: String): Boolean = updatePose(id, name = name)
    fun updatePoseMetadata(id: Long, metadata: String): Boolean = updatePose(id, metadata = metadata)
    fun updatePoseLandmarks(id: Long, landmarksJson: String): Boolean = updatePose(id, landmarksJson = landmarksJson)

    fun deletePose(id: Long): Boolean = dbHelper.deletePose(id)

    fun loadPoses(): List<SavedPoseRecord> = dbHelper.queryPoses()

    fun loadPoses(
        query: String = "",
        sortMode: PoseSortMode = PoseSortMode.LATEST,
        filterMode: PoseFilterMode = PoseFilterMode.ALL
    ): List<SavedPoseRecord> {
        val normalized = query.trim()
        return dbHelper.queryPoses()
            .filter { record ->
                val matchesQuery = normalized.isEmpty() ||
                    record.name.contains(normalized, ignoreCase = true) ||
                    record.gesture.contains(normalized, ignoreCase = true)
                val matchesFilter = when (filterMode) {
                    PoseFilterMode.ALL -> true
                    PoseFilterMode.LIVE -> record.sourceType == PoseSourceType.LIVE.value
                    PoseFilterMode.PHOTO -> record.sourceType == PoseSourceType.PHOTO.value
                }
                matchesQuery && matchesFilter
            }
            .let { list ->
                when (sortMode) {
                    PoseSortMode.LATEST -> list.sortedByDescending { it.savedAt }
                    PoseSortMode.OLDEST -> list.sortedBy { it.savedAt }
                    PoseSortMode.NAME -> list.sortedBy { it.name.lowercase() }
                }
            }
    }

    fun clear() {
        dbHelper.clearAll()
        legacyPrefs.edit().remove(KEY_SAVED_POSES).apply()
    }

    fun loadLatestPose(): SavedPoseRecord? = loadPoses().firstOrNull()

    private fun migrateLegacyDataIfNeeded() {
        val savedPoses = dbHelper.queryPoses()
        if (savedPoses.isNotEmpty()) return

        val raw = legacyPrefs.getString(KEY_SAVED_POSES, null) ?: return
        val legacyList = try {
            JSONArray(raw)
        } catch (_: Exception) {
            null
        } ?: return

        for (i in 0 until legacyList.length()) {
            val obj = legacyList.getJSONObject(i)
            val name = obj.optString("name", "Unnamed")
            val gesture = obj.optString("gesture", "Unknown")
            val confidence = obj.optDouble("confidence", 0.0).toFloat()
            val savedAt = obj.optLong("savedAt", Date().time)
            dbHelper.insertPose(
                name = name,
                gesture = gesture,
                confidence = confidence,
                savedAt = savedAt,
                sourceType = PoseSourceType.LIVE.value,
                thumbnail = null,
                metadata = "",
                landmarksJson = null
            )
        }
    }

    companion object {
        private const val KEY_SAVED_POSES = "saved_poses"
        const val MAX_SAVED_POSES = 100
    }
}
