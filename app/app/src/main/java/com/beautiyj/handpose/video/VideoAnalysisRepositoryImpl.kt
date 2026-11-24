package com.beautiyj.handpose.video

import android.content.Context
import android.net.Uri
import com.beautiyj.data.mediapipe.HandLandmarkerHelper

class VideoAnalysisRepositoryImpl(
    private val context: Context,
    private val extractor: VideoFrameExtractor = VideoFrameExtractor(context),
    private val analyzer: VideoGestureAnalyzer = VideoGestureAnalyzer(context)
) : VideoAnalysisRepository {
    override fun analyze(uri: Uri, onProgress: (Int) -> Unit): VideoGestureAnalysisResult {
        val landmarker = HandLandmarkerHelper.forVideo(context)
        return try {
            analyzer.analyze(uri, landmarker, onProgress)
        } finally {
            landmarker.clearHandLandmarker()
        }
    }
}
