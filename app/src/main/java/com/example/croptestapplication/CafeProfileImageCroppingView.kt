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

class CafeProfileImageCroppingView constructor(context: Context, attrs: AttributeSet) :
    View(context, attrs) {

    private var createBox: Boolean = false
    private var needCrop: Boolean = false

    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1.0f

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            scaleFactor = max(1f, min(scaleFactor, 5.0f))
            this@CafeProfileImageCroppingView.scaleX = scaleFactor
            this@CafeProfileImageCroppingView.scaleY = scaleFactor

            return true
        }
    }

    var scaledBitmap: Bitmap? = null
    var croppedBitmap: Bitmap? = null
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

        scaledBitmap?.let {
            canvas.drawBitmap(it,
                0f,
                (height - scaledBitmap!!.height) / 2f,
                redPaint)
        }
        canvas.save()
        canvas.translate(width / 2f, height / 2f)

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

    private var startX = 0f
    private var startY = 0f
    private val sx = 0f
    private val sy = 135f

    override fun onTouchEvent(event: MotionEvent): Boolean {
//        Log.d("test_by_sungchul", "onTouchEvent: action : ${event.action}")

        if (event.pointerCount > 2) {
            return true
        }

        scaleGestureDetector.onTouchEvent(event)

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
        }
        return true
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
