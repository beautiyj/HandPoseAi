package com.beautiyj.handpose.storage

/**
 * Room 전환용 설계 스케치.
 * 현재 프로젝트는 built-in Kotlin + AGP 환경이라 KAPT 방식의 Room 연동이 호환성 문제를 일으킬 수 있어,
 * 우선 안정적인 기능 구현을 위해 로컬 저장소는 SQLite 헬퍼를 유지하고,
 * 아래 구조를 다음 단계에서 Room/Server DB로 전환할 수 있게 설계해 둡니다.
 */
object PoseSchemaBlueprint {
    const val TABLE_NAME = "saved_poses"
    val SQL_CREATE = """
        CREATE TABLE saved_poses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            gesture TEXT NOT NULL,
            confidence REAL NOT NULL,
            saved_at INTEGER NOT NULL,
            source_type TEXT NOT NULL DEFAULT 'LIVE',
            thumbnail TEXT,
            metadata TEXT DEFAULT '',
            landmarks_json TEXT
        );
    """.trimIndent()

    data class PoseRow(
        val id: Long,
        val name: String,
        val gesture: String,
        val confidence: Float,
        val savedAt: Long,
        val sourceType: String,
        val thumbnail: String?,
        val metadata: String,
        val landmarksJson: String?
    )
}

object ServerPoseSchema {
    const val TABLE_NAME = "pose_records"
    val SQL_POSTGRES = """
        CREATE TABLE pose_records (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            gesture VARCHAR(255) NOT NULL,
            confidence DOUBLE PRECISION NOT NULL,
            source_type VARCHAR(32) NOT NULL,
            saved_at TIMESTAMP NOT NULL DEFAULT NOW(),
            metadata TEXT,
            thumbnail TEXT,
            landmarks_json JSONB,
            user_id BIGINT
        );
    """.trimIndent()
}
