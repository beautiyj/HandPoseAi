from django.shortcuts import render

# 손동작 ai엔진 테스트 후 index.html 틀만 잡아둠 -> views.py 이동 -> 다음은 apps/tracker/urls 경로연결하기

def index(request):
    return render(request, 'tracker/index.html')