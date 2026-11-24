package com.beautiyj.handpose.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoFrameExtractor(
    private val context: Context,
    private val sampleFps: Int = 5,
    private val maxDurationSeconds: Int = 10
) {
    fun forEachFrame(uri: Uri, onFrame: (timestampMs: Long, bitmap: Bitmap) -> Unit) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtMost(maxDurationSeconds * 1000L)
                ?: (maxDurationSeconds * 1000L)
            val intervalUs = 1_000_000L / sampleFps

            var timestampUs = 0L
            while (timestampUs <= durationMs * 1000L) {
                val frame = retriever.getFrameAtTime(timestampUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    val scaled = scaleFrame(frame)
                    onFrame(timestampUs / 1000L, scaled)
                    if (!scaled.isRecycled) scaled.recycle()
                    if (frame !== scaled && !frame.isRecycled) frame.recycle()
                }
                timestampUs += intervalUs
            }
        } finally {
            retriever.release()
        }
    }

    private fun scaleFrame(frame: Bitmap): Bitmap {
        val maxSide = 640
        val scale = minOf(maxSide.toFloat() / frame.width, maxSide.toFloat() / frame.height, 1f)
        if (scale >= 1f) return frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
        val width = (frame.width * scale).toInt().coerceAtLeast(1)
        val height = (frame.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(frame, width, height, true)
    }
}
