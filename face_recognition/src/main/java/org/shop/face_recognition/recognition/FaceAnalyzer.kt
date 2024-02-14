package org.shop.face_recognition.recognition

import android.graphics.PointF
import android.graphics.RectF
import android.media.Image
import android.util.SizeF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.lifecycle.Lifecycle
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

internal class FaceAnalyzer(
    lifecycle: Lifecycle,
    private val preview: PreviewView,
    private val listener: FaceAnalyzerListener?
) : ImageAnalysis.Analyzer {

    private var widthScaleFactor = 1F
    private var heightScaleFactor = 1F

    /**
     *  얼굴인식 모듈자체가 후면 카메라 위주로 되어 있음
     *  따라서 전면카메라로 인식했을 때 좌우로 반전해주는 부분이 필요함
     *  그를 위한 변수 4개
     */
    private var preCenterX = 0F
    private var preCenterY = 0F
    private var preWidth = 0F
    private var preHeight = 0F

    /**
     *  FaceDetectorOptions 빌더로 생성
     *  퍼포먼스 모드: 성능, 정확도를 가장 우선시
     *  윤곽선 받아올 수 있도록 추가
     *  표정도 받아올 수 있도록 추가
     *  최소 0.4 이상만 받아올 수 있게끔 설정
     */
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).setMinFaceSize(0.4F)
        .build()

    private val detector = FaceDetection.getClient(options)

    /**
     *  Detect 상태를 UnDetect 상태로 설정
     */
    private var detectStatus = FaceAnalyzerStatus.UnDetect

    /**
     *  얼굴인식 성공했을 때 가져올 수 있는 Listener
     *
     *  -정리-
     *  처음에 얼굴이 검출됐을 때 처음 UnDetect 상황이면 Detect, 인식됐다고 상태 변경을 해주고, listener를 detect로, progress를 25%
     *  Detect 이면서 왼쪽 눈만 깜빡였다면 상태를 LeftWink로 변경해주고 progress를 50%
     *  LeftWink 상태에서 오른쪽 눈만 깜박였다면 상태를 RightWink로 변경해주고 progress를 75%
     *  마지막으로 RightWink 상태에서 웃으면 상태를 Smile로 변경하고 progress를 100%
     *  인식을 종료하라는 것을 전달해주고 detector를 close
     */
    private val successListener = OnSuccessListener<List<Face>> { faces ->
        val face = faces.firstOrNull()
        if (face != null) {
            if (detectStatus == FaceAnalyzerStatus.UnDetect) {
                detectStatus = FaceAnalyzerStatus.Detect
                listener?.detect()
                listener?.detectProgress(25F, "얼굴을 인식했습니다.\n 왼쪽 눈만 깜빡여주세요.")
            } else if (detectStatus == FaceAnalyzerStatus.Detect
                && (face.leftEyeOpenProbability ?: 0F) > EYE_SUCCESS_VALUE
                && (face.rightEyeOpenProbability ?: 0F) < EYE_SUCCESS_VALUE
            ) {
                detectStatus = FaceAnalyzerStatus.LeftWink
                listener?.detectProgress(50F, "오른쪽 눈만 깜빡여주세요.")
            } else if (detectStatus == FaceAnalyzerStatus.LeftWink
                && (face.leftEyeOpenProbability ?: 0F) < EYE_SUCCESS_VALUE
                && (face.rightEyeOpenProbability ?: 0F) > EYE_SUCCESS_VALUE
            ) {
                detectStatus = FaceAnalyzerStatus.RightWink
                listener?.detectProgress(75F, "활짝 웃어보세요.")
            } else if (detectStatus == FaceAnalyzerStatus.RightWink
                && (face.smilingProbability ?: 0F) > SMILE_SUCCESS_VALUE
            ) {
                detectStatus = FaceAnalyzerStatus.Smile
                listener?.detectProgress(100F, "얼굴 인식이 완료되었습니다.")
                listener?.stopDetect()
                detector.close()
            }

            calDetectSize(face)
        } else if (detectStatus != FaceAnalyzerStatus.UnDetect
            && detectStatus != FaceAnalyzerStatus.Smile
        ) {
            /**
             *  Face가 검출되지 않았을 때 초기값을 변경해줘야 함
             *  Undetect가 아니면서, Status가 Smile이 아닐 때, 즉 맨 처음과 맨 끝이 아닌 중간 단계일 때
             *  중간에 얼굴인식을 잃어버린 것으로 판단하고 처음으로 돌려서 처음부터 다시 시작하도록 유도
             */
            detectStatus = FaceAnalyzerStatus.UnDetect
            listener?.notDetected()
            listener?.detectProgress(0F, "얼굴을 인식하지 못했습니다.\n처음으로 돌아갑니다.")
        }
    }

    /**
     *  얼굴인식이 실패했을 때의 Listener
     */
    private val failureListener = OnFailureListener { e ->
        detectStatus = FaceAnalyzerStatus.UnDetect
    }

    init {
        lifecycle.addObserver(detector)
    }

    override fun analyze(image: ImageProxy) {
        widthScaleFactor = preview.width.toFloat() / image.height
        heightScaleFactor = preview.height.toFloat() / image.width

        detectFaces(image)
    }

    private fun detectFaces(imageProxy: ImageProxy) {
        val image = InputImage.fromMediaImage(
            imageProxy.image as Image,
            imageProxy.imageInfo.rotationDegrees
        )
        detector.process(image).addOnSuccessListener(successListener)
            .addOnFailureListener(failureListener).addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     *  rect 좌표랑 크기, centerX, Y까지 구함
     *  이것을 매번 호출하게 되면 베지에 곡선으로 만들 얼굴인식된 달걀형 마스크가 자글자글해지면서 계속해서 깨지는 것처럼 보일 수 있음
     *  따라서 offset을 두고 control
     */
    private fun calDetectSize(face: Face) {
        /**
         *  박스의 크기 구함
         */
        val rect = face.boundingBox
        val boxWidth = rect.right - rect.left
        val boxHeight = rect.bottom - rect.top

        /**
         *  좌표를 구함
         */
        val left = rect.right.translateX() - (boxWidth / 2)
        val top = rect.top.translateY() - (boxHeight / 2)
        val right = rect.left.translateX() + (boxWidth / 2)
        val bottom = rect.bottom.translateY()

        /**
         *  계산된 영역의 너비와 높이, X, Y(중앙점)들을 계산해주는 변수 추가
         */
        val width = right - left
        val height = bottom - top
        val centerX = left + width / 2
        val centerY = top + height / 2

        if (abs(preCenterX - centerX) > PIVOT_OFFSET
            || abs(preCenterY - centerY) > PIVOT_OFFSET
            || abs(preWidth - width) > SIZE_OFFSET
            || abs(preHeight - height) > SIZE_OFFSET
        ) {
            listener?.faceSize(
                RectF(left, top, right, bottom),
                SizeF(width, height),
                PointF(centerX, centerY)
            )

            /**
             *  preCenterX, preCenterY, preWidth, preHeight 를 저장해서 다음번에 들어왔을 때 개선하기 위해 set
             */
            preCenterX = centerX
            preCenterY = centerY
            preWidth = width
            preHeight = height
        }
    }

    /**
     * X와 Y를 옮겨주는 확장함수
     */
    private fun Int.translateX() = preview.width - (toFloat() * widthScaleFactor)
    private fun Int.translateY() = toFloat() * heightScaleFactor

    companion object {
        /**
         *  예를 들어 윙크를 했을 때 어느 정도 깜빡여야 성공으로 하는가에 대한 기준
         *  EYE : 눈이 0.1 이하로 떨어지면 눈을 감았다고 판단
         *  SMILE : 웃음이 0.8 이상 수치가 들어오면 웃은걸로 판단
         */
        private const val EYE_SUCCESS_VALUE = 0.1F
        private const val SMILE_SUCCESS_VALUE = 0.8F

        /**
         *  offset
         */
        private const val PIVOT_OFFSET = 15
        private const val SIZE_OFFSET = 30
    }
}