package com.beautiyj.domain

import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.beautiyj.domain.usecase.AnalyzeHandUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val useCase = AnalyzeHandUseCase()

    @Test
    fun `analyzeLiveFrame_detects_open_palm`() {
        val result = useCase.analyzeLiveFrame(listOf(buildHandPose(wristY = 0.8f, fingerTipOffset = -0.25f)))

        assertEquals("Open Palm", result.gestureName)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `analyzeLiveFrame_detects_fist`() {
        val result = useCase.analyzeLiveFrame(listOf(buildHandPose(wristY = 0.8f, fingerTipOffset = 0.25f)))

        assertEquals("Fist", result.gestureName)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `analyzeLiveFrame_returns_empty_hand_when_no_pose`() {
        val result = useCase.analyzeLiveFrame(emptyList())

        assertEquals("손 인식 없음", result.gestureName)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `analyzePhoto_uses_same_logic_as_live_frame`() {
        val liveResult = useCase.analyzeLiveFrame(listOf(buildHandPose(wristY = 0.8f, fingerTipOffset = -0.25f)))
        val photoResult = useCase.analyzePhoto(listOf(buildHandPose(wristY = 0.8f, fingerTipOffset = -0.25f)))

        assertEquals(liveResult.gestureName, photoResult.gestureName)
        assertEquals(liveResult.confidence, photoResult.confidence, 0.01f)
    }

    private fun buildHandPose(wristY: Float, fingerTipOffset: Float): HandPoseData {
        val landmarks = MutableList(21) { index ->
            val isFingerTip = index == 8 || index == 12 || index == 16 || index == 20
            val y = if (isFingerTip) wristY + fingerTipOffset else wristY + 0.12f + (index % 5) * 0.04f
            Landmark(x = 0.5f + (index % 4) * 0.04f, y = y, z = 0f)
        }

        return HandPoseData(
            landmarks = landmarks,
            handedness = "Right",
            score = 1.0f
        )
    }
}