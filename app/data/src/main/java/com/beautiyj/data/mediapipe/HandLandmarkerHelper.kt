package com.beautiyj.data.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * [HandLandmarkerHelper]
 * 역할: MediaPipe 외부 라이브러리를 우리 앱의 입맛에 맞게 조정하는 어댑터 클래스.
 */
class HandLandmarkerHelper(
    val context: Context,
    // 러닝모드는 기본 실시간으로 설정, 이후 유지보수성을 위해서도 변수로 추가해둠.
    // 추가안할경우-하드코딩이라 그외 분석코드 추가하려면 귀찮아짐
    // 지금은 파라미터만 추가하면 편하게 활용 가능함 var maxNumHands: Int = 1,이런식으로
    var runningMode: RunningMode = RunningMode.LIVE_STREAM,
    // 러닝모드를 실시간으로 지정할 경우 리스너(변수명이지만 관례적으로 각기 다름) 지정 필요
    // 리스너 : 인터페이스? = 널 허용
    /* 인터페이스에 따른 변수명 관례
    Listener (리스너): 언제 데이터가 올지 모르지만 데이터가 올 때마다 계속 소식을 들어야 합니다.
        예시 - 카메라가 켜진 상태에서 손가락은 실시간으로 계속 움직임, 엔진은 계속 결과를 출력해야함.
              보통 리스너는 실시간 스트리밍처럼 계속해서 데이터 요청이 들어올 때 여럿에게 광고하듯,,,
    Callback (콜백): 심부름시킨 거 다 하면 나중에 응답. 요청과 응답이 1:1이고 일이 끝나면 통로가 닫힘
        예시 - 지금 화면에 보이는 손가락 사진 딱 한 장만 분석해줘 같은 기능에선 콜백.
              딱 한 번의 요청을 보내고 -> 엔진은 분석을 끝낸 뒤 "자, 여기 결과!"라고 한 번(One-time) 응답
              보통 콜백변수는 버튼 클릭이나 데이터 요청 시에 어울림.
    Delegate (델리게이트): 안드로이드에서는 거의 쓰지 않고 ios에서 자주 쓰지만 사용은 한다. 판단을 넘길 때.
        예시 - 엔진이 손가락을 분석했는데, 이게 '확대' 동작인지 '축소' 동작인지 판단해서 화면을 조절해야 해.
              그러나 현재 헬퍼 역할은 수학계산 담당임. 그럼 화면담당을 대리인에게 맡김.
              사용법은 delegate.shouldZoomIn(scaleFactor) 같은 함수를 인터페이스에 담아둠.
              단순히 결과를 받는 게 아니라, 내가 지금 이런 상황인데 어캄? 일 때 대리인에게 의사결정을 맡기는 개념

     */
    val listener: LandmarkerListener? = null,
) {
    // 미디어파이프 엔진 객체
    /* 변수 생명주기 1번 상태임(+미디어파이프는 자체적 비동기처리있어서 코루틴필요X)
    시동 전: 변수는 있지만 내용은 null임. (HandLandmarker? = null 널허용으로설정)
    시동 후: setupHandLandmarker()가 실행되면 비로소 객체가 할당됨.
    사용 중: 언제든 엔진이 꺼질 수 있으니 ?.(세이프 콜)로 안전하게 호출함.
    종료 후: close()를 호출하고 다시 null로 비워서 메모리를 아낌.
     */
    private var handLandmarker: HandLandmarker? = null

    // 생성자 실행문(클래스가 인스턴스화될 때 가장 먼저 실행되는 초기화, 즉 시작세팅임.)
    init {
        setupHandLandmarker()
    }

    /**
     * 엔진 설정 함수
     * 역할: AI의 '뇌'를 깨우고, 실시간 모드로 세팅합니다.
     */
    // 핸드랜드마커 설정하기
    fun setupHandLandmarker() {
        // 1. 가이드라인의 BaseOptions: 모델 파일 경로 설정
        val baseOptionBuilder = BaseOptions.builder()
                               // 모델 asset 폴더 경로 컴패니언상수 지정
            .setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        // 2. 필수 체크: 실시간 모드일 때 리스너가 없으면 개발자에게 에러 표시(=앱크래시발생시키기)
        // throw = raise(파이썬) 강제종료 및 강종메시지
        /* 구글 가이드에서: 실시간 모드일 때 결과 콜백 처리하라고 지시
            (결과를 비동기식으로 수신할 리스너를 설정하려면 resultListener를 호출)
           해당 메시지에 맞게 실시간 모드일 때 수신할 리스너(데이터타입이 인터페이스)가 널이면 강종 처리
         */
        if (runningMode == RunningMode.LIVE_STREAM && listener == null) {
            throw IllegalStateException("runningMode가 LIVE_STREAM일 때 listener(인터페이스) 필요")
        }

        // 안정성을 위해 예외처리구문 활용 (예: 모델파일이없거나 폰사양문제로 엔진이 안켜질 때 사용자알림용)
        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                // 해당 메소드들은 공식규정의 가이드 코드예시에 적혀있음(미디어파이프-구성옵션에서 확인하기)
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setRunningMode(runningMode)

            // 3. 실시간 모드일 때만 비동기 결과 리스너 연결
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnResult) // 성공 시 콜백
                    .setErrorListener(this::returnError)   // 실패 시 콜백
            }


            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)

        } catch (e: Exception) {
            // [실무 필수] 엔진 초기화 실패 시 앱이 죽지 않게 예외 처리
            val errorMsg = "Hand Landmarker 초기화 실패: ${e.message}"
            listener?.onError(errorMsg)
            Log.e(TAG, errorMsg)
        }
    }

    /**
     * 카메라 화면(Bitmap)을 분석기에 넣는 함수
     * 역할: 엔진에 연료(이미지 프레임)를 공급합니다.
     */
    fun detectLiveStream(bitmap: Bitmap) {
        val frameTime = SystemClock.uptimeMillis()
        // MPImageBuilder를 통해 비트맵을 AI용 이미지 객체로 변환
        val mpImage = BitmapImageBuilder(bitmap).build()
        // 비동기로 분석 시작 (결과는 returnResult로 돌아옴)
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    /**
     * 분석 결과 리턴 함수
     * 역할: 구글의 복잡한 원본 데이터를 우리 앱 전용 모델(HandPoseData)로 가공(Mapping)합니다.
     */
    private fun returnResult(result: HandLandmarkerResult, input: MPImage) {
        // [포폴 포인트] 외부 라이브러리 데이터를 도메인 모델로 변환하여 의존성을 낮춤
        val handPoseList = result.landmarks().mapIndexed { index, landmarks ->
            HandPoseData(
                landmarks = landmarks.map { Landmark(it.x(), it.y(), it.z()) },
                handedness = result.handedness()[index][0].categoryName(),
                score = result.handedness()[index][0].score()
            )
        }
        listener?.onResults(handPoseList)
    }

    private fun returnError(error: RuntimeException) {
        listener?.onError(error.message ?: "알 수 없는 에러 발생")
    }

    fun clearHandLandmarker() {
        handLandmarker?.close()
        handLandmarker = null
    }

    // [인터페이스] 외부(Repository/ViewModel)와 통신하는 전용 통로
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(result: List<HandPoseData>)
    }

    // 컴패니언객체: 클래스 내에서 사용하는 상수들 (유지보수성 향상)
    /*회원 목록 리스트.txt 라는 상수를 적을 때 FILE_NAME_MEMBER_LIST / FILE_MEMBER_LIST_JSON처럼써도ㅇㅋ
    접두어(Prefix): 이 상수가 어디 소속인가? (예: FILE, PREF, KEY, EXTRA)
    내용(Content): 무엇에 대한 데이터인가? (예: USER_LIST, MEMBER_INFO)
    접미어(Suffix): 어떤 성격/확장자인가? (예: PATH, URL, ID, TASK)
     */
    companion object {
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        private const val TAG = "HandLandmarkerHelper"

        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }
}


/*

중간중간 주석된부분은 구글가이드기준으로 작성된 부분,
주석 없이 일반 코드는 우선 하드코딩 방식으로+내앱에맞춤형으로 만든 거.
대신 안정성 부분에선 아쉬운 점이 있으니 구글가이드 일부를 차용에서 섞을 것.

package com.beautiyj.data.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.beautiyj.domain.model.HandPoseData
import com.beautiyj.domain.model.Landmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    val context: Context,
    val listener: LandmarkerListener? = null
//  var runningMode: RunningMode = RunningMode.LIVE_STREAM
) {
    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker()
    }

    /*
    fun clearHandLandmarker() {
        handLandmarker?.close()
        handLandmarker = null
    }

    fun isClose(): Boolean = handLandmarker == null
     */
    fun setupHandLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnResult)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    /*
    fun setupHandLandmarker() {
        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setModelAssetPath(MP_HAND_LANDMARKER_TASK)

        if (runningMode == RunningMode.LIVE_STREAM && handLandmarkerHelperListener == null) {
            throw IllegalStateException(
                "runningMode가 LIVE_STREAM일 때 handLandmarkerHelperListener 세팅해야함"
            )
        }

        try {
            val baseOptions = baseOptionBuilder.build()
            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(runningMode)

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                    .setResultListener(this::returnLivestreamResult)
                    .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)

        } catch (e: Exception) {
            val errorMsg = "Hand Landmarker failed to initialize: ${e.message}"
            handLandmarkerHelperListener?.onError(errorMsg)
            Log.e(TAG, errorMsg)
        }
    }
     */

    fun detectLiveStream(bitmap: Bitmap) {
        val frameTime = SystemClock.uptimeMillis()
        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
//        val mpImage = BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnResult(result: HandLandmarkerResult, input: Any) {
        val handPoseList = result.landmarks().mapIndexed { index, landmarks ->
            HandPoseData(
                landmarks = landmarks.map { Landmark(it.x(), it.y(), it.z()) },
                handedness = result.handedness()[index][0].categoryName(),
                score = result.handedness()[index][0].score()
            )
        }
        listener?.onResults(handPoseList)
    }

    /*
    private fun returnLivestreamResult(result: HandLandmarkerResult, input: MPImage) {
        handLandmarkerHelperListener?.onResults(result)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerHelperListener?.onError(error.message ?: "알 수 없는 에러")
    }
     */

    interface LandmarkerListener {
        fun onError(error: String)
//        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(result: List<HandPoseData>)
//        fun onResults(result: HandLandmarkerResult)
    }

    /*
    companion object {
        private const val TAG = "HandLandmarkerHelper"
        private const val MP_HAND_LANDMARKER_TASK = "hand_landmarker.task"
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
    }
     */

}
 */