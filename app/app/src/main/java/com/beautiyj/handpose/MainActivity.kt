package com.beautiyj.handpose

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.beautiyj.handpose.ui.CameraPreviewScreen
import com.beautiyj.handpose.ui.theme.HandPoseAiTheme

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

                // 3. 앱 시작 시 권한 요청
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasCameraPermission) {
                        // 권한 승인 시 카메라 화면 띄움
                        CameraPreviewScreen()
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