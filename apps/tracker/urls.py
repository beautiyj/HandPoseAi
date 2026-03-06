from django.urls import path
from apps.tracker import views

# ex.txt에 자세히 저구어둠. 여기에 경로 다 연결하고 나중에 코어경로는 path('tracker/', include('apps.tracker.urls')), 한줄이면 됨!!

app_name = 'tracker'

urlpatterns = [
    path('', views.index, 'index'), # 'http://127.0.0.1:8000/' 에 연결
]