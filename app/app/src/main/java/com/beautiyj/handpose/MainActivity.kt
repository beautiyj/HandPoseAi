package com.beautiyj.handpose

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beautiyj.handpose.storage.PoseFilterMode
import com.beautiyj.handpose.storage.PoseSortMode
import com.beautiyj.handpose.storage.PoseSourceType
import com.beautiyj.handpose.storage.SavedPoseRecord
import com.beautiyj.handpose.ui.CameraGuideOverlay
import com.beautiyj.handpose.ui.CameraPreviewScreen
import com.beautiyj.handpose.ui.HandPoseOverlay
import com.beautiyj.handpose.ui.theme.HandPoseAiTheme
import com.beautiyj.handpose.viewmodel.MainViewModel
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandPoseAiTheme {
                val viewModel = remember { MainViewModel(applicationContext) }
                var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                var selectedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var selectedPoseId by remember { mutableStateOf<Long?>(null) }
                var poseToDelete by remember { mutableStateOf<SavedPoseRecord?>(null) }
                var poseToEdit by remember { mutableStateOf<SavedPoseRecord?>(null) }
                var editName by remember { mutableStateOf("") }
                var editMetadata by remember { mutableStateOf("") }
                var hasCameraPermission by remember { mutableStateOf(false) }

                val filteredPoses = viewModel.savedPoses
                val activePreviewPose = filteredPoses.firstOrNull { it.id == selectedPoseId } ?: filteredPoses.firstOrNull()
                val selectedPreviewPose = activePreviewPose?.landmarksJson?.let { parseLandmarksJson(it) }

                val photoHandLandmarker = remember {
                    com.beautiyj.data.mediapipe.HandLandmarkerHelper(
                        context = applicationContext,
                        listener = object : com.beautiyj.data.mediapipe.HandLandmarkerHelper.LandmarkerListener {
                            override fun onError(error: String, errorCode: Int) {
                                Toast.makeText(this@MainActivity, "사진 감지 실패: $error", Toast.LENGTH_SHORT).show()
                            }

                            override fun onResults(result: List<com.beautiyj.domain.model.HandPoseData>) {
                                viewModel.analyzePhoto(result)
                            }
                        }
                    )
                }

                DisposableEffect(photoHandLandmarker) {
                    onDispose { photoHandLandmarker.clearHandLandmarker() }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                    if (!isGranted) {
                        Toast.makeText(this, "카메라 권한이 거절되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    selectedImageUri = uri
                    if (uri != null) {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            if (bitmap != null) {
                                selectedPhotoBitmap = bitmap
                                photoHandLandmarker.detectImage(bitmap)
                            } else {
                                viewModel.analyzePhoto(emptyList())
                            }
                        } ?: run { viewModel.analyzePhoto(emptyList()) }
                    }
                }

                val takePhotoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicturePreview()
                ) { bitmap ->
                    if (bitmap != null) {
                        selectedPhotoBitmap = bitmap
                        photoHandLandmarker.detectImage(bitmap)
                    }
                }

                if (poseToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { poseToDelete = null },
                        title = { Text("포즈 삭제") },
                        text = { Text("정말로 '${poseToDelete?.name}' 포즈를 삭제하시겠습니까?\n삭제된 포즈는 복구할 수 없습니다.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val id = poseToDelete?.id ?: return@TextButton
                                viewModel.deletePose(id)
                                poseToDelete = null
                                selectedPoseId = null
                            }) {
                                Text("삭제")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { poseToDelete = null }) {
                                Text("취소")
                            }
                        }
                    )
                }

                if (poseToEdit != null) {
                    AlertDialog(
                        onDismissRequest = { poseToEdit = null },
                        title = { Text("저장된 포즈 수정") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("포즈 이름") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editMetadata,
                                    onValueChange = { editMetadata = it },
                                    label = { Text("메모 / 추가설명") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val current = poseToEdit ?: return@TextButton
                                if (editName.isNotBlank()) {
                                    viewModel.updatePoseName(current.id, editName)
                                }
                                viewModel.updatePoseMetadata(current.id, editMetadata)
                                poseToEdit = null
                            }) {
                                Text("저장")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { poseToEdit = null }) {
                                Text("취소")
                            }
                        }
                    )
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasCameraPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(Color.Black)
                        ) {
                            if (selectedPoseId != null && selectedPreviewPose != null && activePreviewPose != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    HandPoseOverlay(
                                        poseData = selectedPreviewPose,
                                        gestureName = activePreviewPose.gesture,
                                        modifier = Modifier.fillMaxSize(),
                                        overlayLabel = "3D "
                                    )
                                    Text(
                                        text = "저장 포즈: ${activePreviewPose.name}\n${activePreviewPose.gesture} · ${activePreviewPose.sourceType} · ${(activePreviewPose.confidence * 100).toInt()}%",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                                    )
                                }
                            } else if (selectedPhotoBitmap != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = selectedPhotoBitmap!!.asImageBitmap(),
                                        contentDescription = "분석된 손 사진",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    HandPoseOverlay(
                                        poseData = viewModel.currentHandPose,
                                        gestureName = viewModel.uiState.gestureName,
                                        modifier = Modifier.fillMaxSize(),
                                        overlayLabel = "3D "
                                    )
                                }
                            } else {
                                CameraPreviewScreen(viewModel)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "권장: 손이 선명하게 보이는 고화질 사진, 얼굴 배경은 배제, 손은 프레임 중앙에 위치",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = viewModel.customPoseName,
                                    onValueChange = { viewModel.updateCustomPoseName(it) },
                                    label = { Text("포즈 이름") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        val thumbnail = selectedPhotoBitmap?.let { bitmapToBase64(it) }
                                        val ok = viewModel.saveCurrentPose(thumbnail = thumbnail)
                                        if (!ok) {
                                            Toast.makeText(this@MainActivity, "저장 가능한 포즈 수는 100개를 초과할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("현재 포즈 저장")
                                    }
                                    Button(onClick = { galleryLauncher.launch("image/*") }) { Text("갤러리") }
                                    Button(onClick = { takePhotoLauncher.launch(null) }) { Text("촬영") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.updatePoseFilterMode(PoseFilterMode.ALL) }) { Text("전체") }
                                    Button(onClick = { viewModel.updatePoseFilterMode(PoseFilterMode.LIVE) }) { Text("실시간") }
                                    Button(onClick = { viewModel.updatePoseFilterMode(PoseFilterMode.PHOTO) }) { Text("사진") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { viewModel.updatePoseSortMode(PoseSortMode.LATEST) }) { Text("최신순") }
                                    Button(onClick = { viewModel.updatePoseSortMode(PoseSortMode.OLDEST) }) { Text("오래된순") }
                                    Button(onClick = { viewModel.updatePoseSortMode(PoseSortMode.NAME) }) { Text("제목순") }
                                }

                                OutlinedTextField(
                                    value = viewModel.poseQuery,
                                    onValueChange = { viewModel.updatePoseQuery(it) },
                                    label = { Text("검색 / 필터") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (viewModel.savedPoses.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("저장된 포즈", fontWeight = FontWeight.Bold, color = Color.White)
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(viewModel.savedPoses) { saved ->
                                                val thumbnailBitmap = saved.thumbnail?.let { decodeBase64Bitmap(it) }
                                                val isSelected = saved.id == (selectedPoseId ?: activePreviewPose?.id)

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().background(
                                                        if (isSelected) Color(0xFF1F2937) else Color(0xFF111827)
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (thumbnailBitmap != null) {
                                                        Image(
                                                            bitmap = thumbnailBitmap.asImageBitmap(),
                                                            contentDescription = "포즈 썸네일",
                                                            modifier = Modifier
                                                                .fillMaxWidth(0.18f)
                                                                .padding(8.dp)
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${saved.name} · ${saved.gesture}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${saved.sourceType} · ${(saved.confidence * 100).toInt()}% · ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", saved.savedAt)}",
                                                            color = Color(0xFFCBD5E1)
                                                        )
                                                        if (saved.metadata.isNotBlank()) {
                                                            Text(text = saved.metadata, color = Color(0xFF93C5FD))
                                                        }
                                                    }

                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Button(onClick = {
                                                            selectedPoseId = saved.id
                                                        }) { Text("선택") }
                                                        Button(onClick = {
                                                            poseToEdit = saved
                                                            editName = saved.name
                                                            editMetadata = saved.metadata
                                                        }) { Text("수정") }
                                                        Button(onClick = { poseToDelete = saved }) { Text("삭제") }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                            Text(text = "카메라 권한이 필요합니다.")
                        }
                    }
                }
            }
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val bytes = outputStream.toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

private fun decodeBase64Bitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

private fun parseLandmarksJson(json: String): com.beautiyj.domain.model.HandPoseData? {
    if (json.isBlank()) return null
    val points = json.split("|")
        .mapNotNull { point ->
            val values = point.split(",")
            if (values.size < 3) return@mapNotNull null
            val x = values[0].toFloatOrNull() ?: return@mapNotNull null
            val y = values[1].toFloatOrNull() ?: return@mapNotNull null
            val z = values[2].toFloatOrNull() ?: return@mapNotNull null
            com.beautiyj.domain.model.Landmark(x, y, z)
        }
    if (points.isEmpty()) return null
    return com.beautiyj.domain.model.HandPoseData(
        landmarks = points,
        handedness = "Unknown",
        score = 1.0f
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CameraScreenPreview() {
    HandPoseAiTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CameraGuideOverlay()
            Text(
                text = "카메라 미리보기 영역 (실제 기기에서 실행하세요)",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}