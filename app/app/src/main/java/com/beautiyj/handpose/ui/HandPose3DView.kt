package com.beautiyj.handpose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.beautiyj.domain.model.HandPoseData

@Composable
fun HandPose3DView(
    poseData: HandPoseData?,
    gestureName: String,
    modifier: Modifier = Modifier
) {
    HandPoseOverlay(
        poseData = poseData,
        gestureName = gestureName,
        modifier = modifier,
        overlayLabel = "3D "
    )
}
