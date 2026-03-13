package com.beautiyj.data.repository

import com.beautiyj.data.mediapipe.HandLandmarkerHelper
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.repository.HandRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HandRepositoryImpl(
    private val handLandmarkerHelper: HandLandmarkerHelper
) : HandRepository {

    // 도메인 설계도대로 실제 데이터를 '흐름(Flow)'으로 바꿔주는 구현부
    override fun getHandPoseStream(): Flow<List<HandPoseData>> = callbackFlow {

        // 1. AI 엔진으로부터 결과를 받을 리스너(콜백) 정의
        val listener = object : HandLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String) {
                // 에러 발생 시 스트림 닫기 (장고의 500 Error 처리와 비슷)
                close(Exception(error))
            }

            override fun onResults(result: List<HandPoseData>) {
                // 실시간 좌표가 들어오면 스트림으로 쏴줍니다 (장고의 실시간 알림 비슷)
                trySend(result)
            }
        }

        // 2. 엔진 설정 및 가동
        // 여기서 아까 assets에 넣은 task 파일을 불러오게 됩니다.
        handLandmarkerHelper.setupHandLandmarker()

        // 주의: 실시간 리스너를 연결해주는 로직이 helper에 필요합니다.
        // (아까 작성한 helper 코드에 리스너 설정 로직이 포함되어 있습니다.)

        // 3. 앱이 종료되거나 화면을 나갈 때 엔진 정지 (리소스 해제)
        awaitClose {
            // handLandmarkerHelper.clear() 같은 정리 함수 호출 (필요시)
        }
    }
}