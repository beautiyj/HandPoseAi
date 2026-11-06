package com.beautiyj.handpose

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beautiyj.handpose.ui.CameraPreviewScreen
import com.beautiyj.handpose.ui.theme.HandPoseAiTheme
import com.beautiyj.handpose.viewmodel.MainViewModel


// 메인 액티비티

// onCreate(savedInstanceState: Bundle?) : 앱 시작 시 호출되는 메인 액티비티의 생명주기 메서드,
// 화면 세팅과 카메라 권한 요청, 카메라 화면 표시 여부 결정 역할
// rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
// : 카메라 권한 요청 결과를 받는 용도의 런처, 권한 허용/거부 처리 및 거부 시 안내 메시지 띄우기
// LaunchedEffect(Unit) : 앱 시작 시 권한 요청을 트리거(자동으로 요청), 화면이 열리자마자 권한 체크하는 용도
// setContent { HandPoseAiTheme { ... } } : Compose UI 화면 세팅, HandPoseAiTheme 테마 적용,
// Scaffold 내부에서 카메라 권한 상태에 따라 카메라 화면 또는 안내 문구 표시
// CameraPreviewScreen(viewModel) : 카메라 권한 승인 시 카메라 화면 표시(거부 시 사용자 안내), MainViewModel 전달


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HandPoseAiTheme {

                // 보통 스코폴드 내부부터 ui화면
                /*Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }*/

                // 비즈니스로직들
                val viewModel = remember { MainViewModel() }
                var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

                // 1. 카메라 권한 상태관리
                var hasCameraPermission by remember { mutableStateOf(false) }

                // 2. 권한 요청 런처
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
                        Toast.makeText(this, "사진 선택 완료: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
                        viewModel.analyzePhoto(emptyList())
                    }
                }

                // 3. 앱 시작 시 권한 요청
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasCameraPermission) {
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            CameraPreviewScreen(viewModel)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { galleryLauncher.launch("image/*") }
                                ) {
                                    Text("사진 업로드로 손 인식")
                                }
                            }
                        }
                    } else {
                        // 권한 거부 시 안내 문구
                        Box(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "카메라 권한이 필요합니다.")
                        }
                    }
                }


            }
        }
    }
}

/*
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HandPoseAiTheme {
        Greeting("Android")
    }
}
*/

// 프리뷰코드
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CameraScreenPreview() {
    HandPoseAiTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 실제 카메라 하드웨어는 프리뷰에서 작동하지 않으므로
            // 디자인 레이아웃만 확인하는 용도로 사용합니다.
            Text(text = "카메라 미리보기 영역 (실제 기기에서 실행하세요)")
        }
    }
}