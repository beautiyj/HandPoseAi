package com.beautiyj.handpose.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_poses")
data class SavedPoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "gesture")
    val gesture: String,
    @ColumnInfo(name = "confidence")
    val confidence: Float,
    @ColumnInfo(name = "saved_at")
    val savedAt: Long,
    @ColumnInfo(name = "source_type")
    val sourceType: String = PoseSourceType.LIVE.value,
    @ColumnInfo(name = "thumbnail")
    val thumbnail: String? = null,
    @ColumnInfo(name = "metadata")
    val metadata: String = "",
    @ColumnInfo(name = "landmarks_json")
    val landmarksJson: String? = null
) {
    fun toRecord(): SavedPoseRecord = SavedPoseRecord(
        id = id,
        name = name,
        gesture = gesture,
        confidence = confidence,
        savedAt = savedAt,
        sourceType = sourceType,
        thumbnail = thumbnail,
        metadata = metadata,
        landmarksJson = landmarksJson
    )
}
