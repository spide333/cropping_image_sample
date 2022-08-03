package com.example.croptestapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.ViewCompat
import kotlin.math.abs
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

    private var needBitmap = false
    private var createBox: Boolean = false
    private var needCrop: Boolean = false

    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1.0f
    private var minScale = 0.75f
    private var largeScale = 1f

    private var defaultPivotX = 0f
    private var defaultPivotY = 0f

    init {
        defaultPivotX = pivotX
        defaultPivotY = pivotY
//        pivotX = 0f
//        pivotY = 0f
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            scaleFactor = max(minScale, min(scaleFactor, 500.0f))
            this@CafeProfileImageCroppingView.scaleX = scaleFactor
            this@CafeProfileImageCroppingView.scaleY = scaleFactor

            return true
        }
    }

    private val mCurrentViewport = RectF(0f, 0f, 500f, 500f)

    private val mContentRect: Rect? = null

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            // Scrolling uses math based on the viewport (as opposed to math using pixels).

            mContentRect?.apply {
                // Pixel offset is the offset in screen pixels, while viewport offset is the
                // offset within the current viewport.
                val viewportOffsetX = distanceX * mCurrentViewport.width() / width()
                val viewportOffsetY = -distanceY * mCurrentViewport.height() / height()

                // Updates the viewport, refreshes the display.
                setViewportBottomLeft(
                    mCurrentViewport.left + viewportOffsetX,
                    mCurrentViewport.bottom + viewportOffsetY
                )
            }

            return true
        }
    }

    private fun setViewportBottomLeft(x: Float, y: Float) {
        /*
         * Constrains within the scroll range. The scroll range is simply the viewport
         * extremes (AXIS_X_MAX, etc.) minus the viewport size. For example, if the
         * extremes were 0 and 10, and the viewport size was 2, the scroll range would
         * be 0 to 8.
         */

        val curWidth: Float = mCurrentViewport.width()
        val curHeight: Float = mCurrentViewport.height()
        val newX: Float = Math.max(0f, min(x, 500f - curWidth))
        val newY: Float = Math.max(0f + curHeight, min(y, 500f))

        mCurrentViewport.set(newX, newY - curHeight, newX + curWidth, newY)

        // Invalidates the View to update the display.
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private val gestureDetector = GestureDetector(context, mGestureListener)


    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }


//    var scaledBitmap: Bitmap? = null
    var targetBitmap: Bitmap? = null

    var sourceBitmap: Bitmap? = null
        set(value) {
            field = value

            needBitmap = true
            createBox = false
            needCrop = false


            imageWidth = sourceBitmap?.width ?: 0
            imageHeight = sourceBitmap?.height ?: 0

            layoutParams.width = imageWidth
            layoutParams.height = imageHeight

            standardX = (width - imageWidth) / 2
            standardY = (height - imageHeight) / 2
            Log.d("test_by_sungchul", "rect w : $imageWidth, h : $imageHeight")
            rect = Rect(0, 0, 500, 500)
            scaleBitmap()
            invalidate()
        }

    private fun scaleBitmap() {


        // crop 할 때 원본 비트맾에서 해야 화질 유지
        // .scale 리소스 많이 먹음. 5만, 10만 이러면 죽음
        // 37줄 처럼 처리하는게 나음
    }

    private var redPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 20f
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

    var startSave = -1

    override fun onDraw(canvas: Canvas) {
        Log.d("test_by_sungchul", "onDraw: 1")
        super.onDraw(canvas)
        canvas.drawColor(Color.RED)
        startSave = canvas.save()
        drawSource(canvas)
    }

    private fun createBox(canvas: Canvas) {

//            Log.d("test_by_sungchul", "onDraw 3 : length : $length, bitmap w : ${sourceBitmap?.width}, bitmap h : ${sourceBitmap?.height}")

        val dx = abs((width - x))
        val dy = abs((height - y))

        Log.d("test_by_sungchul",
            "before restore : x : $x, y : $y, width : $width, height : $height")
        canvas.restoreToCount(startSave)
        Log.d("test_by_sungchul",
            "after restore : x : $x, y : $y, width : $width, height : $height")

//        val length = width / (scaleFactor)

        val length = width * minScale
        val move = width * scaleFactor / 2

        Log.d("test_by_sungchul",
            "- createBox dx : $dx, dy : $dy, x : $x, y : $y, scaleFactor : $scaleFactor, width : $width, height : $height")

//        canvas.translate(x / scaleFactor + width / 2f, y / scaleFactor + height / 2f)

//        canvas.translate(dx, dy)

        canvas.translate(x, y)

        canvas.drawLine(0f, 0f, length, 0f, redPaint)
        canvas.drawLine(length, 0f, length, length, redPaint)
        canvas.drawLine(length, length, 0f, length, redPaint)
        canvas.drawLine(0f, length, 0f, 0f, redPaint)

//        canvas.restore()
        canvas.drawRect(0f, 0f, 200f, 200f, greenPaint)
        invalidate()
        if (needCrop) {
//                scaledBitmap ?: return
//                val cropped = Bitmap.createBitmap(scaledBitmap!!,
//                    max((scaledBitmap!!.width / 2 - length + dx).toInt(), 0),
//                    max((scaledBitmap!!.height / 2 - length + dy).toInt(), 0),
//                    2 * length.toInt(),
//                    2 * length.toInt())
//                    .scale((300 / scaleFactor).toInt(), (300 / scaleFactor).toInt())
//
//                canvas.drawBitmap(cropped, -length, 0 - height / (scaleFactor * 2f), greenPaint)
        }
    }

    private fun drawSource(canvas: Canvas) {
        Log.d("test_by_sungchul", "drawSource: needBitmap : $needBitmap")

        sourceBitmap?.let {

//            canvas.drawBitmap(it, 0f, (height - scaledBitmap!!.height) / 2f, redPaint)
            val scaleX = width.toFloat() / sourceBitmap!!.width.toFloat()
            val scaleY = (height.toFloat() + sy) / sourceBitmap!!.height.toFloat()

            val minScale = min(scaleX, scaleY)
            val maxScale = max(scaleX, scaleY)

//            val needScale = maxScale / minScale
            val needScale = maxScale


//            val needScale = maxScale

            this.minScale = needScale

            this.scaleX = needScale
            this.scaleY = needScale

            scaleFactor = needScale

            canvas.save()
            canvas.scale(minScale, minScale, 0f, 0f)
            canvas.drawBitmap(it, 0f, 0f, redPaint)

//            val dx = abs(width * (1f - scaleFactor) / 2f)
//            val dy = abs(height * (1f - scaleFactor) / 2f)

            val dx = abs((width - x) * (1f - scaleFactor))
            val dy = abs((height - y) * (1f - scaleFactor))
            Log.d("test_by_sungchul",
                "- scaleX : $scaleX, scaleY : $scaleY, minScale : $minScale, needScale : $needScale, curX : $x, curY : $y, dx : $dx, dy : $dy")


            largeScale = maxScale


//            canvas.drawRect(dx, dy, dx + 200f, dy + 200f, greenPaint)

            if (createBox) {
                createBox(canvas)
            }

//            pivotX = dx
//            pivotY = dy


            needBitmap = false
        }
    }

    private var startX = 0f
    private var startY = 0f
    private val sx = 0f
    private val sy = 135f

    private var mLastTouchX = 0f
    private var mLastTouchY = 0f

    private var mPosX = 0f
    private var mPosY = 0f

    private var mActivePointerId = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("test_by_sungchul", "scale : $scaleFactor, w : $width, h : $height")
//        Log.d("test_by_sungchul", "onTouchEvent: action : ${event.action}")

        // 위에 제스처 디텍터

        val dx = abs(width * (1f - scaleFactor) / 2f) + defaultPivotX
        val dy = abs(height * (1f - scaleFactor) / 2f) + defaultPivotY


        val leftLimit = -dx
        val rightLimit = dx

        val topLimit = -dy
        val bottomLimit = dy + sy

        Log.d("test_by_sungchul",
            "- curX : $x, curY : $y, left: $leftLimit, right : $rightLimit, top : $topLimit, bottom : $bottomLimit")

        scaleGestureDetector.onTouchEvent(event)

        // return gestureDetector.onTouchEvent(event) || scaleGestureDetector.onTouchEvent(event)
        // 보통 return scaleGestureDetector.onTouchEvent(event) 이렇게 한다.

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                event.actionMasked.also { pointerIndex ->
                    mLastTouchX = event.getX(pointerIndex)
                    mLastTouchY = event.getY(pointerIndex)
                }

                // Save the ID of this pointer (for dragging)
                mActivePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                // Find the index of the active pointer and fetch its position
                val (x: Float, y: Float) =
                    event.findPointerIndex(mActivePointerId).let { pointerIndex ->
                        // Calculate the distance moved
                        event.getX(pointerIndex) to event.getY(pointerIndex)
                    }


                // 0.5f 일 때 x : -270, y : -474.25

                val nextX = this.x + x - mLastTouchX
                val nextY = this.y + y - mLastTouchY

                this.x = when {
                    nextX < 0f -> {
                        Log.d("test_by_sungchul", "onTouchEvent: x 1")
                        0f
                    }
                    nextX > dx -> {
                        Log.d("test_by_sungchul", "onTouchEvent: x 2")
                        dx
                    }
                    else -> {
                        Log.d("test_by_sungchul", "onTouchEvent: x 3")
                        nextX
                    }
                }

                this.y = when {
                    nextY < 0f -> {
                        Log.d("test_by_sungchul", "onTouchEvent: y 1")
                        0f
                    }
                    nextY > dy -> {
                        Log.d("test_by_sungchul", "onTouchEvent: y 2")
                        dy
                    }
                    else -> {
                        Log.d("test_by_sungchul", "onTouchEvent: y 3")
                        nextY
                    }
                }

//                this.x = max(-dx, min(this.x + x - mLastTouchX, dx))
//                this.y = max(-dy, min(this.y + y - mLastTouchY, dy + sy))
//
//                this.x = max(-dx, this.x + x - mLastTouchX)
//                this.y = max(-dy, this.y + y - mLastTouchY)

//                invalidate()

                // Remember this touch position for the next move event
//                mLastTouchX = x
//                mLastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {

                event.actionIndex.also { pointerIndex ->
                    event.getPointerId(pointerIndex)
                        .takeIf { it == mActivePointerId }
                        ?.run {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
                            mLastTouchX = event.getX(newPointerIndex)
                            mLastTouchY = event.getY(newPointerIndex)
                            mActivePointerId = event.getPointerId(newPointerIndex)
                        }
                }
            }
        }
        return true


        // event 순서 2번
        return true // consume 메세지, true면 내가 처리하겠다는 의미. false면 다른 view한테
    }

    // event 순서 1번
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }

    // event 순서 3번
//    private val onTouchListener(motionEvent: MotionEvent?): Unit {
//
//    }


    fun onFirstButtonClicked() {
        createBox = true
        invalidate()
//        needCrop = false
//        x += width * scaleFactor
//        y += height * scaleFactor
//        invalidate()
    }

    fun onSecondButtonClicked() {
        needCrop = true
        invalidate()
    }

    fun onStartButtonClicked() {
        needBitmap = true
    }
}
