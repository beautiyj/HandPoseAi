package com.beautiyj.handpose.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PoseDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS saved_poses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "gesture TEXT NOT NULL, " +
                "confidence REAL NOT NULL, " +
                "saved_at INTEGER NOT NULL, " +
                "source_type TEXT NOT NULL DEFAULT 'LIVE', " +
                "thumbnail TEXT, " +
                "metadata TEXT DEFAULT '', " +
                "landmarks_json TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE saved_poses ADD COLUMN source_type TEXT NOT NULL DEFAULT 'LIVE'")
            db.execSQL("ALTER TABLE saved_poses ADD COLUMN thumbnail TEXT")
            db.execSQL("ALTER TABLE saved_poses ADD COLUMN metadata TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE saved_poses ADD COLUMN landmarks_json TEXT")
        }
    }

    fun insertPose(
        name: String,
        gesture: String,
        confidence: Float,
        savedAt: Long,
        sourceType: String = PoseSourceType.LIVE.value,
        thumbnail: String? = null,
        metadata: String = "",
        landmarksJson: String? = null
    ): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("gesture", gesture)
            put("confidence", confidence)
            put("saved_at", savedAt)
            put("source_type", sourceType)
            put("thumbnail", thumbnail)
            put("metadata", metadata)
            put("landmarks_json", landmarksJson)
        }
        return writableDatabase.insert("saved_poses", null, values)
    }

    fun updatePose(
        id: Long,
        name: String? = null,
        gesture: String? = null,
        confidence: Float? = null,
        sourceType: String? = null,
        metadata: String? = null,
        thumbnail: String? = null,
        landmarksJson: String? = null
    ): Boolean {
        val values = ContentValues().apply {
            name?.let { put("name", it) }
            gesture?.let { put("gesture", it) }
            confidence?.let { put("confidence", it) }
            sourceType?.let { put("source_type", it) }
            metadata?.let { put("metadata", it) }
            thumbnail?.let { put("thumbnail", it) }
            landmarksJson?.let { put("landmarks_json", it) }
        }
        if (values.size() == 0) return false
        val updated = writableDatabase.update("saved_poses", values, "id = ?", arrayOf(id.toString()))
        return updated > 0
    }

    fun deletePose(id: Long): Boolean {
        val count = writableDatabase.delete("saved_poses", "id = ?", arrayOf(id.toString()))
        return count > 0
    }

    fun queryPoses(): List<SavedPoseRecord> {
        val result = mutableListOf<SavedPoseRecord>()
        val cursor = readableDatabase.query(
            "saved_poses",
            arrayOf("id", "name", "gesture", "confidence", "saved_at", "source_type", "thumbnail", "metadata", "landmarks_json"),
            null,
            null,
            null,
            null,
            "saved_at DESC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                result.add(
                    SavedPoseRecord(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        gesture = c.getString(c.getColumnIndexOrThrow("gesture")),
                        confidence = c.getFloat(c.getColumnIndexOrThrow("confidence")),
                        savedAt = c.getLong(c.getColumnIndexOrThrow("saved_at")),
                        sourceType = c.getString(c.getColumnIndexOrThrow("source_type")),
                        thumbnail = c.getString(c.getColumnIndexOrThrow("thumbnail")),
                        metadata = c.getString(c.getColumnIndexOrThrow("metadata")),
                        landmarksJson = c.getString(c.getColumnIndexOrThrow("landmarks_json"))
                    )
                )
            }
        }
        return result
    }

    fun clearAll() {
        writableDatabase.delete("saved_poses", null, null)
    }

    companion object {
        private const val DATABASE_NAME = "hand_pose_db"
        private const val DATABASE_VERSION = 2
    }
}
