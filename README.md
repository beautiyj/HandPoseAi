# 🖐️ HandPoseAi

MediaPipe와 3D SceneView를 활용한 모바일 온디바이스 실시간 모션 캡처 및 3D 모델링 연동 시스템입니다.

---

## 🏗️ System Architecture

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant App as 안드로이드 앱<br/>(MediaPipe + SceneView)
    participant Room as 로컬 Room DB
    participant Server as Spring Boot 서버
    participant PG as PostgreSQL

    User->>App: 카메라/사진/영상 입력
    App->>App: MediaPipe AI 좌표 추출
    App->>Room: 추출된 좌표 데이터 로컬 저장
    Note over App, Room: 네트워크 없이도 온디바이스 저장 가능
    
    opt 서버 동기화 시
        App->>Server: HTTP REST API (모션 데이터 전송)
        Server->>PG: 데이터 영속화 저장
        PG-->>Server: 응답
        Server-->>App: 성공 리턴
    end
