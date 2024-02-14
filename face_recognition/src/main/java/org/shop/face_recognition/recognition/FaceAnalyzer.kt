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
     *  얼굴인식 성공했을 때 가져올 수 있는 Listener
     */
    private val successListener = OnSuccessListener<List<Face>> { faces ->

    }

    /**
     *  얼굴인식이 실패했을 때의 Listener
     */
    private val failureListener = OnFailureListener { e ->

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
}