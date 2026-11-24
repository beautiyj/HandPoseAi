package com.beautiyj.handpose.video

object VideoRenderFramePolicy {
    const val MAX_DURATION_SECONDS = 10
    const val SAMPLE_FPS = 5
    const val MAX_FRAME_SIDE_PX = 640
    const val MAX_STORED_FRAMES = 50

    fun frameIntervalMs(): Long = 1000L / SAMPLE_FPS

    fun isWithinFrameBudget(frameCount: Int): Boolean =
        frameCount <= MAX_STORED_FRAMES

    fun estimatedArgbBitmapBytes(width: Int, height: Int): Long =
        width.toLong().coerceAtLeast(1) * height.toLong().coerceAtLeast(1) * 4L
}
