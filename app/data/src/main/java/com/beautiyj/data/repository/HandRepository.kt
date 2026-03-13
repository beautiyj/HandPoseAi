package com.beautiyj.domain.repository

import com.beautiyj.domain.model.HandPoseData
import kotlinx.coroutines.flow.Flow

interface HandRepository {
    fun getHandPoseStream(): Flow<List<HandPoseData>>
}