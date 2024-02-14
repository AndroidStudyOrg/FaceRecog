# FaceRecog
얼굴인식 서비스
- 베젤 곡선을 활용하여 얼굴형 마스크를 씌우고 그 모양을 따라서 ProgressBar 표현
- 카메라 프리뷰 활성화
    - CameraX
- 얼굴인식 모듈 설치
    - 인식된 얼굴의 모양, 눈, 코, 입의 위치를 파악하고 보여지는 얼굴이 실제 사람인지 사진인지 판단
- 얼굴인식 범위 Mask 씌우기
    - 인식된 얼굴의 대략적인 모양의 구멍을 뚫어 제대로 얼굴 인식 중인지 체크
- 간단한 요구사항을 통한 상호작용
- 인식 진행사항 출력
    - 인식 완료까지 얼마나 남았는지 얼굴 모양을 따라서 ProgressBar로 출력
 
## Module
- Camera Preview를 띄워서 얼굴을 인식하고 진행상황을 Main Module에 전달

## CameraX Preview
- 화면에 출력하기 위해 사용
- 전면 카메라 뷰만 사용 예정
- CameraX
    - 더 쉬운 카메라 앱 개발을 위해 빌드된 Jetpack 라이브러리
    - Camera2를 사용하므로 Android 5.0(API 21)까지만 지원
    - 미리보기, 이미지 분석, 이미지 캡쳐, 동영상 캡쳐 지원
    - 생명주기를 인식 -> 별도로 생명주기 관리를 해주지 않아도 됨
    - 장치 호환성 문제를 해결함으로써 기기별 분기코드 감소
    - 특정 기기에 종속되는 Bokeh, HDR 등 지원

## Permission
- Camera를 활성화 시키기 위한 권한
- 간단하게 Camera 권한만 얻을 수 있도록 구현

## Google Vision
- Google Vision에서 제공하는 이미지 분석 툴
- 얼굴 인식에 대한 내용 구성
- [ML Kit 얼굴인식 API](https://developers.google.com/ml-kit/vision/face-detection?hl=ko)
    - 카메라 속 얼굴을 인식하고 주요 얼굴 특징을 식별하며, 인식된 얼굴의 윤곽을 가져올 수 있음
    - 얼굴을 감지할 수는 있지만 사람인지 인식하지는 못한다. 따라서 ML Kit에서 제공해주는 정보를 바탕으로 간단한 Mission을 통해서 해당 얼굴이 사람인지 아닌지 판단
    - ML Kit 얼굴인식을 사용하면 셀카나 인물사진 꾸미기, 유저 사진으로 아바타 생성 등 작업에 필요한 정보를 얻을 수 있음
    - ML Kit은 실시간으로 얼굴인식을 할 수 있음. 따라서 유저의 표정에 응답하는 영상 채팅이나 게임과 같은 앱에서 사용 가능
    - 주요 기능
        - **얼굴 특징 인식 및 위치 찾기** : 감지된 얼굴의 눈, 귀, 볼, 코, 입의 좌표를 확인할 수 있음
        - **얼굴 특징의 윤곽 가져올 수 있음**
        - **감지된 얼굴과 눈, 눈썹, 입술, 코의 윤곽을 가져올 수 있음**
        - **표정인식** : 웃고있는지, 눈을 감고있는지 판단할 수 있음
        - **동영상 프레임에서 얼굴 추적 가능** : 감지된 얼굴의 고유 식별자를 가져오고 식별자는 호출 전체에서 일관됨. 동영상 스트림에서 특징 인물의 이미지를 조작할 수 있음
        - **동영상 프레임에서 실시간으로 처리** : 얼굴인식은 기기에서 실행되고 동영상 조작과 같은 실시간 어플리케이션에서 사용하기에 충분히 빠르다
      
## CustomView - Paint
- Paint를 이용해서 CustomView를 구성해서 얼굴 모양의 Mask 씌움

## Bezier Curves
- 3차 베지에 곡선을 이용해서 얼굴형을 그림
- 베지에 곡선(Bezier Curve)
    - 매끄러운 곡선을 그리기 위한 것
    - 설정한 각 조절점(P0, P1, P2, P3)을 따라 이은 직선(Q0,Q1, Q2)
    - 이 직선(Q0,Q1, Q2)을 따라 이은 직선(R0, R1)
    - 이 직선(R0, R1)위에서 일정하게 움직이는 점(B)이 그리는 궤적

## PathMeasure
- 얼굴형 모양에 따라 ProgressBar 출력
