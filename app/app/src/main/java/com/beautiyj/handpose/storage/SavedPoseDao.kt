package com.beautiyj.handpose.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SavedPoseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: SavedPoseEntity): Long

    @Update
    fun update(entity: SavedPoseEntity): Int

    @Query("DELETE FROM saved_poses WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("SELECT * FROM saved_poses ORDER BY saved_at DESC")
    fun getAll(): List<SavedPoseEntity>

    @Query("SELECT * FROM saved_poses WHERE id = :id LIMIT 1")
    fun getById(id: Long): SavedPoseEntity?

    @Query("DELETE FROM saved_poses")
    fun clearAll()

    @Query("SELECT COUNT(*) FROM saved_poses")
    fun count(): Int
}
