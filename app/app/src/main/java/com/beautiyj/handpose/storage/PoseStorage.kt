package com.beautiyj.handpose.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

data class SavedPoseRecord(
    val name: String,
    val gesture: String,
    val confidence: Float,
    val savedAt: Long
)

class PoseStorage(context: Context) {
    private val prefs = context.getSharedPreferences("hand_pose_store", Context.MODE_PRIVATE)

    fun savePose(name: String, gesture: String, confidence: Float) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isEmpty()) {
            "사용자 포즈 ${loadPoses().size + 1}"
        } else {
            trimmed
        }

        val current = loadPoses().toMutableList()
        current.add(
            SavedPoseRecord(
                name = finalName,
                gesture = gesture,
                confidence = confidence,
                savedAt = Date().time
            )
        )

        val array = JSONArray()
        current.forEach { pose ->
            val obj = JSONObject()
            obj.put("name", pose.name)
            obj.put("gesture", pose.gesture)
            obj.put("confidence", pose.confidence)
            obj.put("savedAt", pose.savedAt)
            array.put(obj)
        }

        prefs.edit().putString(KEY_SAVED_POSES, array.toString()).apply()
    }

    fun loadPoses(): List<SavedPoseRecord> {
        val raw = prefs.getString(KEY_SAVED_POSES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<SavedPoseRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    SavedPoseRecord(
                        name = obj.optString("name", "Unnamed"),
                        gesture = obj.optString("gesture", "Unknown"),
                        confidence = obj.optDouble("confidence", 0.0).toFloat(),
                        savedAt = obj.optLong("savedAt", 0L)
                    )
                )
            }
            result.sortedByDescending { it.savedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_SAVED_POSES).apply()
    }

    companion object {
        private const val KEY_SAVED_POSES = "saved_poses"
    }
}
