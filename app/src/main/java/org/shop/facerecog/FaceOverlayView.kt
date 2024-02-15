package org.shop.facerecog

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.SizeF
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     *  사용할 Paint 정의
     *  배경은 black에 alpha 90
     */
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 90
        style = Paint.Style.FILL
    }

    /**
     *  FacePaint: 윤곽선을 따라서 대시로 점선을 그림
     */
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 10F
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    /**
     *  MaskPaint
     */
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 가운데만 뻥 뚫리도록
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    private val facePath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawOverlay(canvas)
    }

    /**
     *  얼굴 인식 Module에서 들어오는 size를 그대로 넘겨줄 예정이기에 3개의 Param을 받는다
     */
    fun setSize(rectF: RectF, sizeF: SizeF, pointF: PointF) {
        /**
         *  얼굴인식 Module에서 위아래가 조금씩 작게 나오기 때문에 추가로 이 부분을 더 늘려주는 작업을 하기 위해 offset을 임의로 추가
         */
        val topOffset = sizeF.width / 2
        val bottomOffset = sizeF.width / 5

        /**
         *  첫번째 cubicTo
         *  가운데 상단부터 시작해서 오른쪽으로 x y x y x y 로 그리게 된다
         *  그래서 오른쪽 위, 오른쪽 아래, 정 가운데 아래쪽으로 반원 그리게 됨.
         *
         *  두번째 cubicTo
         *  가운데 하단부터 시작해서 시계방향으로 위로 올라가기 시작해서 가운데 상단까지 그려지게 된다.
         */
        with(facePath) {
            reset()
            moveTo(pointF.x, rectF.top) // 포인트를 정 가운데 위에다가 set한다
            cubicTo(
                rectF.right + topOffset,
                rectF.top,
                rectF.right + bottomOffset,
                rectF.bottom,
                pointF.x,
                rectF.bottom
            )
            cubicTo(
                rectF.left - bottomOffset,
                rectF.bottom,
                rectF.left - topOffset,
                rectF.top,
                pointF.x,
                rectF.top
            )
            close()
        }

        postInvalidate()
    }

    fun reset() {
        facePath.reset()
        invalidate()
    }

    private fun drawOverlay(canvas: Canvas) {
        canvas.drawRect(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat(), backgroundPaint)
        canvas.drawPath(facePath, maskPaint)
        canvas.drawPath(facePath, facePaint)
    }
}