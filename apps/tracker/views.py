from django.shortcuts import render

# 손동작 ai엔진 테스트 후 index.html 틀만 잡아둠 -> views.py 이동 -> 다음은 apps/tracker/urls 경로연결하기
# 안드로이드의 뷰모델과 비슷한 역할을 하지만 안드로이드와 세부적인 면은 다름. 장고에선 db접근해서 응답하는 처리기 역할 백엔드.
# views.py는 사용자의 요청(리퀘스트) -> 서버의 응답(리스폰스) 사이 중간 관리자 역할을 하는 곳
# render(requset, template_name = '') 이건 해당 요청을 뒤의 html쪽에 데이터 담아서 다시 리턴하라는 거.

def index(request):
    return render(request, 'tracker/index.html')