from django.urls import path
from apps.tracker import views

# 그다음 코어 urls 연결

app_name = 'tracker'

urlpatterns = [
    path('', views.index, name='index'), # 'http://127.0.0.1:8000/' 에 연결
]