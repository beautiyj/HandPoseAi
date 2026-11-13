package com.beautiyj.handpose.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedPoseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PoseDatabase : RoomDatabase() {
    abstract fun savedPoseDao(): SavedPoseDao

    companion object {
        @Volatile
        private var INSTANCE: PoseDatabase? = null

        fun getInstance(context: Context): PoseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PoseDatabase::class.java,
                    "hand_pose_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
