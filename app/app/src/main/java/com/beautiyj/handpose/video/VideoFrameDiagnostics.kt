package com.beautiyj.handpose.video

import android.os.SystemClock
import java.util.Locale

class VideoFrameDiagnostics {
    private val startedAtMs = SystemClock.elapsedRealtime()
    private var processedFrames = 0

    fun recordFrame() {
        processedFrames++
    }

    fun snapshot(frameWidth: Int, frameHeight: Int): VideoFrameDiagnosticSnapshot {
        val elapsedMs = (SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(1)
        val averageFps = processedFrames * 1000f / elapsedMs
        val bitmapBytes = VideoRenderFramePolicy.estimatedArgbBitmapBytes(frameWidth, frameHeight)
        return VideoFrameDiagnosticSnapshot(
            processedFrames = processedFrames,
            elapsedMs = elapsedMs,
            averageFps = averageFps,
            activeFrameBitmapBytes = bitmapBytes
        )
    }
}

data class VideoFrameDiagnosticSnapshot(
    val processedFrames: Int,
    val elapsedMs: Long,
    val averageFps: Float,
    val activeFrameBitmapBytes: Long
) {
    fun displayLabel(): String = String.format(
        Locale.US,
        "프레임 %d · %.1f fps · 예상 활성 비트맵 %d KB",
        processedFrames,
        averageFps,
        activeFrameBitmapBytes / 1024L
    )
}
