import mediapipe as mp
import cv2

# 0305기준 테스트코드만 기재

class HandAnalyzer:
    def __init__(self):
        # 미디어파이프 손 인식 도구 세팅
        self.mp_hands = mp.solutions.hands
        self.hands = self.mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            min_detection_confidence=0.7,
            min_tracking_confidence=0.5
        )
        self.mp_draw = mp.solutions.drawing_utils

# --- 테스트 실행 코드 ---
if __name__ == "__main__":
    analyzer = HandAnalyzer()
    cap = cv2.VideoCapture(0) # 0번은 기본 웹캠

    print("카메라를 켭니다... 'q'를 누르면 종료됩니다.")

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        # 손가락 좌표 인식 과정
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = analyzer.hands.process(rgb_frame)

        # 손이 화면에 보이면 마디마다 점과 선을 그림
        if results.multi_hand_landmarks:
            for hand_lms in results.multi_hand_landmarks:
                analyzer.mp_draw.draw_landmarks(
                    frame, hand_lms, analyzer.mp_hands.HAND_CONNECTIONS
                )

        cv2.imshow("Hand Tracking Test", frame)

        # q 키를 누르면 루프 탈출
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()