from django.contrib import admin
from .models import SocialAccount, HandPose


#  from import 의 경우, 상위폴더가 아닌 같은 tracker 패키지 내의 models.py 를 부를 경우 . 을 붙여줘야함

# 순서 2. admin.py에는 models.py에서 지정한 데이터 테이블 중 필요 데이터를 선별하여 관리자 페이지에 띄운다.
# html과 달리 사용자에게 보여지는 게 아니며, 이미 만들어진 장고의 관리자페이지에서 선별된 데이터 리스트를 확인하는 절차.
# 일단 관리자페이지에서 소셜로그인 회원 리스트, 손동작 데이터 리스트 정도만 추가 완. 이후 수정 가능
# 손동작 ai엔진 테스트 후 index.html 틀만 잡아둠 -> views.py 이동

@admin.register(SocialAccount)
class SocialAccountAdmin(admin.ModelAdmin):
    list_display = ('user', 'provider', 'social_id')


@admin.register(HandPose)
class HandPoseAdmin(admin.ModelAdmin):
    list_display = ('id', 'user', 'pose_name', 'source_choice', 'created_at')
    list_filter = ('source_choice', 'created_at')
    search_fields = ('pose_name',)
