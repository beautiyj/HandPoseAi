// apps/tracker/static/tracker/js/hand_ai.js

const videoElement = document.getElementsByClassName('input_video')[0];
const canvasElement = document.getElementsByClassName('output_canvas')[0];
const canvasCtx = canvasElement.getContext('2d');

/**
 * 안드로이드의 OnNavigationItemSelected처럼
 * MediaPipe가 손을 인식할 때마다 호출되는 콜백 함수입니다.
 */
function onResults(results) {
    // 1. 캔버스 초기화 (화면 지우기)
    canvasCtx.save();
    canvasCtx.clearRect(0, 0, canvasElement.width, canvasElement.height);

    // 2. 웹캠 영상을 캔버스에 그리기
    canvasCtx.drawImage(results.image, 0, 0, canvasElement.width, canvasElement.height);

    // 3. 손이 인식되었다면 점과 선 그리기
    if (results.multiHandLandmarks) {
        for (const landmarks of results.multiHandLandmarks) {
            // 점(Landmarks) 그리기 (안드로이드의 Paint 객체 활용과 유사)
            drawConnectors(canvasCtx, landmarks, HAND_CONNECTIONS,
                           {color: '#00FF00', lineWidth: 5});
            drawLandmarks(canvasCtx, landmarks, {color: '#FF0000', lineWidth: 2});

            // [나중 과제] 여기서 추출된 landmarks(21개 좌표)를
            // fetch()를 이용해 장고 서버(views.py)로 보낼 예정입니다.
            // console.log(landmarks);
        }
    }
    canvasCtx.restore();
}

/**
 * MediaPipe Hands 설정 (안드로이드 SDK 설정과 대응)
 */
const hands = new Hands({locateFile: (file) => {
    return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`;
}});

hands.setOptions({
    maxNumHands: 2,            // 인식할 최대 손 개수
    modelComplexity: 1,        // 모델 정확도 (0 또는 1)
    minDetectionConfidence: 0.5, // 인식 문턱값
    minTrackingConfidence: 0.5  // 추적 문턱값
});

hands.onResults(onResults);

/**
 * 카메라 실행 (안드로이드의 CameraProvider와 대응)
 */
const camera = new Camera(videoElement, {
    onFrame: async () => {
        await hands.send({image: videoElement});
    },
    width: 1280,
    height: 720
});
camera.start();