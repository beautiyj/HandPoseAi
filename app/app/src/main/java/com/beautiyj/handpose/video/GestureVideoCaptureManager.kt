package com.beautiyj.handpose.video

import android.os.SystemClock
import kotlin.math.min

class GestureVideoCaptureManager {
    private var startTimestampMs: Long = 0L
    private var isRecording = false
    private val sampleIntervalMs = 200L
    private var recordedSamples = mutableListOf<GestureVideoSample>()

    fun beginClip(): GestureVideoClip {
        startTimestampMs = SystemClock.elapsedRealtime()
        isRecording = true
        recordedSamples.clear()
        return GestureVideoClip(
            startedAtMs = startTimestampMs,
            durationSeconds = 10.0,
            sampleFrameIntervalMs = sampleIntervalMs,
            status = "recording"
        )
    }

    fun recordSample(gestureName: String, confidence: Float, timestampMs: Long) {
        if (!isRecording) return
        val elapsedMs = timestampMs - startTimestampMs
        if (elapsedMs < 0) return
        if (recordedSamples.size >= MAX_SAMPLES) return
        recordedSamples.add(
            GestureVideoSample(
                elapsedMs = elapsedMs,
                gestureName = gestureName,
                confidence = confidence,
                sampledAt = timestampMs
            )
        )
    }

    fun endClip(): GestureVideoClip {
        val elapsedMs = SystemClock.elapsedRealtime() - startTimestampMs
        isRecording = false
        val safeDuration = min(elapsedMs.toDouble() / 1000.0, 10.0)
        val summary = summarizeRecordedSamples()
        return GestureVideoClip(
            startedAtMs = startTimestampMs,
            durationSeconds = safeDuration,
            sampleFrameIntervalMs = sampleIntervalMs,
            status = "completed",
            sampleCount = recordedSamples.size,
            lastGesture = summary.dominantGesture,
            averageConfidence = summary.averageConfidence
        )
    }

    fun isRecording(): Boolean = isRecording

    fun sampleFrameCount(durationSeconds: Double): Int {
        return ((durationSeconds * 1000.0) / sampleIntervalMs).toInt().coerceAtLeast(1)
    }

    fun recordedSamples(): List<GestureVideoSample> = recordedSamples.toList()

    fun summarizeRecordedSamples(): GestureVideoSummary {
        if (recordedSamples.isEmpty()) {
            return GestureVideoSummary(
                dominantGesture = "없음",
                averageConfidence = 0f,
                sampleCount = 0
            )
        }

        val grouped = recordedSamples.groupBy { it.gestureName }
        val dominant = grouped.maxByOrNull { it.value.size }
        val avg = recordedSamples.map { it.confidence }.average().toFloat()
        return GestureVideoSummary(
            dominantGesture = dominant?.key ?: "없음",
            averageConfidence = avg,
            sampleCount = recordedSamples.size
        )
    }

    companion object {
        const val MAX_SAMPLES = 50
    }
}

data class GestureVideoClip(
    val startedAtMs: Long,
    val durationSeconds: Double,
    val sampleFrameIntervalMs: Long,
    val status: String,
    val sampleCount: Int = 0,
    val lastGesture: String = "없음",
    val averageConfidence: Float = 0f
) {
    val maxDurationSeconds: Double = 10.0

    fun displayLabel(): String = "10초 영상 클립 · ${durationSeconds}초 · ${status} · ${sampleCount}개 샘플 · ${lastGesture}"
}

data class GestureVideoSample(
    val elapsedMs: Long,
    val gestureName: String,
    val confidence: Float,
    val sampledAt: Long
)

data class GestureVideoSummary(
    val dominantGesture: String,
    val averageConfidence: Float,
    val sampleCount: Int
)
