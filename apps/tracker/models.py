from django.db import models
from django.contrib.auth.models import User

# 소셜로그인 사용자 테이블
class SocialAccount(models.Model):
    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name='social_account',
        verbose_name="소셜 계정"
    )

    provider = models.CharField(
        verbose_name="google/kakao provider",
        max_length=50,
    )

    social_id = models.CharField(
        verbose_name="소셜 사용자 고유 ID",
        max_length=255,
        unique=True,
    )

    def __str__(self):
        # 아이디를 등록하지 않은 소셜로그인 사용자의 경우
        if self.user.username == self.social_id:
            user_label = f"미등록 유저({self.social_id})"
        else:
            user_label = self.user.username
        return f"[{self.provider.upper()}] {user_label} (ID: {self.social_id})"


# 손동작 테이블
class HandPose(models.Model):
    # 유저클래스는 기본적으로 장고에서 생성해주고 아이디비번은 필수, 이메일 퍼스트네임 라스트네임은 선택및빈칸허용임
    # 유저클래스와 1:1 연결           계정 삭제 시 데이터 같이 삭제
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        null=True,
        blank=True,
    )

    # 선택지 3가지 필드. 튜플은 순서대로 db 저장값, 화면 출력값
    SOURCE_CHOICES = [
        ('webcam', '웹캠으로 촬영하기'),
        ('image', '사진 첨부하기'),
        ('video', '영상 첨부하기'),
    ]

    source_choice = models.CharField(
        verbose_name="데이터 선택지 3가지",
        default='webcam',
        max_length=50,
        choices=SOURCE_CHOICES,
        null=True,
        blank=True,
    )

    # MediaPipe의 랜드마크 데이터 JSON 형태 _ 좌표 데이터
    landmarks_data = models.JSONField(
        verbose_name="원본 좌표",
        help_text="미디어 파이프의 랜드마크 데이터: 21개 손가락 마디 좌표",
    )

    # Three.js 연산용 데이터 JSON 형태 _ 회전값 데이터. 미디어파이프 좌표를 바탕으로 계산한 회전값이 들어갈 예정.
    # 3D 모델용 가공 데이터고 Three.js를 위한 계산값임.     좌표만 먼저 저장하고 연산 결과는 나중에 넣기 가능
    rotation_data = models.JSONField(
        verbose_name="회전값 데이터",
        help_text="쓰리제이에스를 위한 계산값(3D 모델용 가공 데이터)",
        null=True,
        blank=True,
    )

    # 관리자용 메타정보 ex)브이, 주먹, 보, 하트
    pose_name = models.CharField(
        verbose_name="포즈명",
        max_length=100,
        blank=True,
    )

    # 데이터 저장 시간 자동 기록
    created_at = models.DateTimeField(
        verbose_name="생성 일시",
        auto_now_add=True,
    )

    # 관리자용 메타 클래스
    class Meta:
        verbose_name = "손동작 데이터"
        verbose_name_plural = "손동작 데이터 목록"
        ordering = ["-created_at"]  #최신순 정렬

    def __str__(self):
        name = self.pose_name or '미분류 포즈'
        source = self.get_source_choice_display()   # 튜플 내의 대칭값으로 출력해주는 장고 메소드 get_필드명_display()
        return f"[{source}] {name} - {self.created_at.strftime('%Y-%m-%d %H:%M')}"
