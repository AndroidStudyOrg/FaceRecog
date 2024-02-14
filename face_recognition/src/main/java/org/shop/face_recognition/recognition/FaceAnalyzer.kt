package org.shop.face_recognition.recognition

import android.media.Image
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

internal class FaceAnalyzer(
    lifecycle: Lifecycle,
    private val preview: PreviewView,
    private val listener: FaceAnalyzerListener?
) : ImageAnalysis.Analyzer {

    private var widthScaleFactor = 1F
    private var heightScaleFactor = 1F

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

    companion object {
        /**
         *  예를 들어 윙크를 했을 때 어느 정도 깜빡여야 성공으로 하는가에 대한 기준
         *  EYE : 눈이 0.1 이하로 떨어지면 눈을 감았다고 판단
         *  SMILE : 웃음이 0.8 이상 수치가 들어오면 웃은걸로 판단
         */
        private const val EYE_SUCCESS_VALUE = 0.1F
        private const val SMILE_SUCCESS_VALUE = 0.8F
    }
}