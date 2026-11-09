package com.beautiyj.handpose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.beautiyj.domain.model.HandPoseData

@Composable
fun HandPose3DView(
    poseData: HandPoseData?,
    gestureName: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (poseData != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width * 0.5f
                val centerY = size.height * 0.58f
                val scaleX = size.width * 0.72f
                val scaleY = size.height * 0.72f

                val projectedPoints = poseData.landmarks.map { landmark ->
                    val depth = landmark.z.coerceIn(-1f, 1f)
                    val x = centerX + ((landmark.x - 0.5f) * scaleX * (1.2f + depth * 0.45f))
                    val y = centerY + ((landmark.y - 0.5f) * scaleY * (1.1f - depth * 0.35f))
                    Offset(x, y)
                }

                val connections = listOf(
                    0 to 1, 1 to 2, 2 to 3, 3 to 4,
                    0 to 5, 5 to 6, 6 to 7, 7 to 8,
                    5 to 9, 9 to 10, 10 to 11, 11 to 12,
                    9 to 13, 13 to 14, 14 to 15, 15 to 16,
                    13 to 17, 17 to 18, 18 to 19, 19 to 20,
                    0 to 17, 0 to 5
                )

                connections.forEach { (fromIndex, toIndex) ->
                    val from = projectedPoints[fromIndex]
                    val to = projectedPoints[toIndex]
                    drawLine(
                        color = Color(0xFF5EEAD4),
                        start = from,
                        end = to,
                        strokeWidth = 4f
                    )
                }

                projectedPoints.forEachIndexed { index, point ->
                    val baseColor = when {
                        index == 0 -> Color(0xFF56CCF2)
                        index in listOf(8, 12, 16, 20) -> Color(0xFFFFD166)
                        else -> Color(0xFFB8F2E6)
                    }
                    drawCircle(
                        color = baseColor,
                        radius = 7f,
                        center = point
                    )
                }
            }
        }

        Text(
            text = "3D ${gestureName}",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
