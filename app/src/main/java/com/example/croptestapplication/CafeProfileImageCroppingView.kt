package com.example.croptestapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.min

/**
    1. 사진 불러온다.
    2. 화면 크기에 맞게 사진 크기 조정(계산)
    3. (축소할 수 있는 최소크기 결정 필요) (계산) - 큰 이미지 고려 (화면 사이즈의 3배수 넘지 않도록)
    4. 화면 터치 이벤트 처리
    5. 크롭 할 위치 계산
    6. 크롭 -> 비트맵

 load, updateUI, clear, ...
 */
class CafeProfileImageCroppingView constructor(context: Context, attrs: AttributeSet) :
    View(context, attrs) {

    private var createBox: Boolean = false
    private var needCrop: Boolean = false

    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1.0f

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            scaleFactor = max(0.5f, min(scaleFactor, 5.0f))
            this@CafeProfileImageCroppingView.scaleX = scaleFactor
            this@CafeProfileImageCroppingView.scaleY = scaleFactor

            return true
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.setOnTouchListener (onTouchListener)
    }


//    var scaledBitmap: Bitmap? = null
    var targetBitmap: Bitmap? = null

    var sourceBitmap: Bitmap? = null
        set(value) {
            field = value

            createBox = false
            needCrop = false

            imageWidth = sourceBitmap?.width ?: 0
            imageHeight = sourceBitmap?.height ?: 0
            standardX = (width - imageWidth) / 2
            standardY = (height - imageHeight) / 2
            Log.d("test_by_sungchul", "rect w : $imageWidth, h : $imageHeight")
            rect = Rect(0, 0, 500, 500)
            scaleBitmap()
            invalidate()
        }

    private fun scaleBitmap() {
        val scaleX = width.toFloat() / sourceBitmap!!.width.toFloat()
        val scaleY = height.toFloat() / sourceBitmap!!.height.toFloat()

        val scale = min(scaleX, scaleY)

        Log.d("test_by_sungchul",
            "scaleBitmap: s x : ${sourceBitmap!!.width * scale}, s y : ${sourceBitmap!!.height * scale}")

        scaledBitmap = Bitmap.createBitmap(sourceBitmap!!)
            .scale((sourceBitmap!!.width * scale).toInt(), (sourceBitmap!!.height * scale).toInt())

        // crop 할 때 원본 비트맾에서 해야 화질 유지
        // .scale 리소스 많이 먹음. 5만, 10만 이러면 죽음
        // 37줄 처럼 처리하는게 나음
    }

    private var redPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 1f
    }

    private var greenPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 10f
    }

    private var imageWidth = sourceBitmap?.width ?: 0
    private var imageHeight = sourceBitmap?.height ?: 0

    private var standardX = 0
    private var standardY = 0

    var rectf = RectF(0f, 0f, 100f, 100f)
    var rect = Rect()

    override fun onDraw(canvas: Canvas) {
        Log.d("test_by_sungchul", "onDraw: 1")
        super.onDraw(canvas)
        canvas.drawColor(Color.RED)

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale()
        val matrix = Matrix()
        canvas.concat(matrix)


        drawSource(canvas)

        canvas.restore()

        if (createBox) {
            Log.d("test_by_sungchul",
                "onDraw: 3, x : $x, y : $y, width : $width, height : $height, scale : $scaleFactor")

            val length = width / (2 * scaleFactor)

//            Log.d("test_by_sungchul", "onDraw 3 : length : $length, bitmap w : ${sourceBitmap?.width}, bitmap h : ${sourceBitmap?.height}")

            val dx = -(x - sx) / scaleFactor
            val dy = -(y - sy) / scaleFactor
            canvas.translate(dx, dy)
            canvas.drawLine(-length, -length, length, -length, greenPaint)
            canvas.drawLine(length, -length, length, length, greenPaint)
            canvas.drawLine(length, length, -length, length, greenPaint)
            canvas.drawLine(-length, length, -length, -length, greenPaint)

            if (needCrop) {
                scaledBitmap ?: return
                val cropped = Bitmap.createBitmap(scaledBitmap!!,
                    max((scaledBitmap!!.width / 2 - length + dx).toInt(), 0),
                    max((scaledBitmap!!.height / 2 - length + dy).toInt(), 0),
                    2 * length.toInt(),
                    2 * length.toInt())
                    .scale((300 / scaleFactor).toInt(), (300 / scaleFactor).toInt())

                canvas.drawBitmap(cropped, -length, 0 - height / (scaleFactor * 2f), greenPaint)
            }
        }
    }

    private fun drawSource(canvas: Canvas) {
        scaledBitmap?.let {
//            canvas.drawBitmap(it, 0f, (height - scaledBitmap!!.height) / 2f, redPaint)
            canvas.drawBitmap(it, 0f, 0f, redPaint)
        }
    }

    private var startX = 0f
    private var startY = 0f
    private val sx = 0f
    private val sy = 135f

    override fun onTouchEvent(event: MotionEvent): Boolean {
//        Log.d("test_by_sungchul", "onTouchEvent: action : ${event.action}")

        if (event.pointerCount > 2) {
            return true
        }

        // 위에 제스처 디텍터
        scaleGestureDetector.onTouchEvent(event)

        // return gestureDetector.onTouchEvent(event) || scaleGestureDetector.onTouchEvent(event)
        // 보통 return scaleGestureDetector.onTouchEvent(event) 이렇게 한다.

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.actionIndex == 0) {
                    startX = event.x
                    startY = event.y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.actionIndex == 0) {
//                    Log.d("test_by_sungchul", "onTouchEvent: x : ${event.x}, y : ${event.y}")
                    x += event.x - startX
                    y += event.y - startY
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                // 처리하다가 중간에 없어진 경우 (화면 밖으로 나가는 경우)
            }
        }
        // event 순서 2번
        return true // consume 메세지, true면 내가 처리하겠다는 의미. false면 다른 view한테
    }

    // event 순서 1번
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }

    // event 순서 3번
    private val onTouchListener(motionEvent: MotionEvent?): Unit {

    }


    fun createBox() {
        createBox = true
        needCrop = false
        invalidate()
    }

    fun crop() {
        needCrop = true
        invalidate()
    }
}
