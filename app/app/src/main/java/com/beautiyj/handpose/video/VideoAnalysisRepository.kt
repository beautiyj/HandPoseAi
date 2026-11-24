package com.beautiyj.handpose.video

import android.net.Uri

interface VideoAnalysisRepository {
    fun analyze(
        uri: Uri,
        onProgress: (Int) -> Unit = {}
    ): VideoGestureAnalysisResult
}
