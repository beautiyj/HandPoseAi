from django.contrib import admin
from django.urls import path, include

# include 를 사용하면 ('') 루트주소로들어오는 모든 요청은 해당 주소의 경로설정을 참고함(간략해짐~!)

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', include('apps.tracker.urls')), # 모든 메인 주소를 tracker 앱으로 넘김! 구구절절 코어경로에 쓸 필요 없음.
]