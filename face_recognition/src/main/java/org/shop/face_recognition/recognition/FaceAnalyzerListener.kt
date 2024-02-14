package org.shop.face_recognition.recognition

import android.graphics.PointF
import android.graphics.RectF
import android.util.SizeF

interface FaceAnalyzerListener {

    /**
     *  얼굴이 인식됐을 때
     */
    fun detect()

    fun stopDetect()

    /**
     *  인식이 안됐을 때
     */
    fun notDetected()

    /**
     *  어느정도 인식이 진행됐는지 체크
     */
    fun detectProgress(progress: Float, message: String)

    /**
     *  인식된 얼굴의 속성값들이나 크기 등을 넘겨준다
     */
    fun faceSize(rectF: RectF, sizeF: SizeF, pointF: PointF)
}