package org.shop.facerecog

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
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

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 10F
    }

    /**
     *  MaskPaint
     */
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // 가운데만 뻥 뚫리도록
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    private val facePath = Path()
    private var progress = 0F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        /**
         *  순서에 주의
         *  Progress를 Overlay 다음에 그려야 점선위에 표현이 된다
         *  순서가 바뀌게 되면 Progress가 먼저 그려지기 때문에 제대로 표현되지 않는다
         */
        drawOverlay(canvas)
        drawProgress(canvas)
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

    /**
     *  모듈에서 Progress를 던져줄 때 여기로 들어와서 이것을 Animation 처리해가지고 onDraw를 실행
     *  Animation 처리를 위해 ValueAnimator를 사용
     */
    fun setProgress(progress: Float) {
        ValueAnimator.ofFloat(this.progress, progress).apply {
            duration = ANIMATE_DURATION
            addUpdateListener {
                this@FaceOverlayView.progress = it.animatedValue as Float
                invalidate()
            }
        }.start()
    }

    fun reset() {
        facePath.reset()
        progress = 0F
        invalidate()
    }

    private fun drawOverlay(canvas: Canvas) {
        canvas.drawRect(0F, 0F, canvas.width.toFloat(), canvas.height.toFloat(), backgroundPaint)
        canvas.drawPath(facePath, maskPaint)
        canvas.drawPath(facePath, facePaint)
    }

    private fun drawProgress(canvas: Canvas) {
        val measure = PathMeasure(facePath, true)
        val pathLength = measure.length
        val total = pathLength - (pathLength * (progress / 100))    // 실제 그려줘야 하는 total 값

        /**
         *  Dash 형태로 위에 덧칠
         */
        val pathEffect = DashPathEffect(floatArrayOf(pathLength, pathLength), total)

        progressPaint.pathEffect = pathEffect

        canvas.drawPath(facePath, progressPaint)
    }

    companion object {
        private const val ANIMATE_DURATION = 500L
    }
}