package com.beautiyj.handpose.ui

import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.beautiyj.handpose.video.VideoGestureAnalysisResult
import kotlinx.coroutines.delay

@Composable
fun VideoGesturePlayback(
    uriString: String,
    result: VideoGestureAnalysisResult,
    modifier: Modifier = Modifier
) {
    var currentPositionMs by remember(uriString) { mutableStateOf(0L) }

    val holder = remember(uriString) { FrameLayoutKey() }

    LaunchedEffect(uriString) {
        while (true) {
            delay(50)
            val position = holder.videoView?.currentPosition?.toLong() ?: 0L
            if (position != currentPositionMs) currentPositionMs = position
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    val videoView = VideoView(context)
                    addView(videoView)
                    holder.videoView = videoView
                    videoView.setVideoURI(android.net.Uri.parse(uriString))
                    videoView.setOnPreparedListener { it.start() }
                }
            },
            update = {
                val videoView = holder.videoView
                if (videoView != null && currentPositionMs >= 0) {
                    val position = videoView.currentPosition.toLong()
                    if (position != currentPositionMs) currentPositionMs = position
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        val frame = result.frames.minByOrNull { kotlin.math.abs(it.timestampMs - currentPositionMs) }
        HandFilamentView(
            poseData = frame?.poseData,
            modifier = Modifier.fillMaxSize()
        )
        HandPoseOverlay(
            poseData = frame?.poseData,
            gestureName = frame?.gestureName ?: "대기 중",
            modifier = Modifier.fillMaxSize(),
            overlayLabel = "영상 "
        )
    }
}

private class FrameLayoutKey {
    var videoView: VideoView? = null
}
