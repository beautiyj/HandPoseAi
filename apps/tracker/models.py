from django.db import models
from django.contrib.auth.models import User

class SocialAccount(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='social_account')
    provider = models.CharField(max_length=50)      # google, kakao privider
    social_id = models.CharField(max_length=255, unique=True)
    def __str__(self):
        return f"{self.user.username} {self.provider}

class HandPose(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, null=True, blank=True)
    # 유저클래스는 기본적으로 장고에서 생성해주고 아이디비번은 필수, 이메일 퍼스트네임 라스트네임은 선택및빈칸허용임