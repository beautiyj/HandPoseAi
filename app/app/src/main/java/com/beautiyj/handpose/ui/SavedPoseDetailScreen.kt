package com.beautiyj.handpose.ui

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.beautiyj.handpose.storage.SavedPoseRecord

@Composable
fun SavedPoseDetailScreen(
    pose: SavedPoseRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOriginal by remember(pose.id) { mutableStateOf(false) }
    val poseData = parseLandmarksJson(pose.landmarksJson)
    val thumbnailBitmap = pose.thumbnail?.let { decodeBase64Bitmap(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
    ) {
        if (showOriginal && thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = "원본 손 이미지",
                modifier = Modifier.fillMaxSize()
            )
            if (poseData != null) {
                HandPoseOverlay(
                    poseData = poseData,
                    gestureName = pose.gesture,
                    modifier = Modifier.fillMaxSize(),
                    overlayLabel = "오버레이 "
                )
            }
        } else if (poseData != null) {
            HandPoseOverlay(
                poseData = poseData,
                gestureName = pose.gesture,
                modifier = Modifier.fillMaxSize(),
                overlayLabel = "3D "
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC111827))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = pose.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "인식 결과: ${pose.gesture} · ${(pose.confidence * 100).toInt()}%",
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = "소스: ${pose.sourceType} · ${DateFormat.format("yyyy-MM-dd HH:mm", pose.savedAt)}",
                    color = Color(0xFFCBD5E1)
                )
                if (pose.metadata.isNotBlank()) {
                    Text(
                        text = "메모: ${pose.metadata}",
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val baseModifier = Modifier.weight(1f)
                if (thumbnailBitmap != null) {
                    Button(
                        onClick = { showOriginal = !showOriginal },
                        modifier = baseModifier
                    ) {
                        Text(if (showOriginal) "3D 보기" else "원본+오버레이")
                    }
                }
                TextButton(
                    onClick = onEdit,
                    modifier = baseModifier
                ) { Text("수정") }
                TextButton(
                    onClick = onDelete,
                    modifier = baseModifier
                ) { Text("삭제") }
                TextButton(
                    onClick = onClose,
                    modifier = baseModifier
                ) { Text("닫기") }
            }
        }
    }
}

private fun parseLandmarksJson(json: String?): HandPoseData? {
    if (json.isNullOrBlank()) return null
    val points = json.split("|")
        .mapNotNull { rawPoint ->
            val values = rawPoint.split(",")
            if (values.size < 3) return@mapNotNull null
            val x = values[0].toFloatOrNull() ?: return@mapNotNull null
            val y = values[1].toFloatOrNull() ?: return@mapNotNull null
            val z = values[2].toFloatOrNull() ?: return@mapNotNull null
            Landmark(x, y, z)
        }
    if (points.isEmpty()) return null
    return HandPoseData(
        landmarks = points,
        handedness = "Unknown",
        score = 1.0f
    )
}

private fun decodeBase64Bitmap(base64: String): android.graphics.Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}
