package com.android.example.cameraxbasic

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import kotlinx.coroutines.flow.merge


class CustomOverlayView: View {

    private val paint = Paint()
    private val pathEffect = DashPathEffect(floatArrayOf(15f, 5f), 0F)
    private var mRect: RectF = RectF()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.color = Color.YELLOW
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        paint.pathEffect = pathEffect
        val displayMetrics = Resources.getSystem().displayMetrics
        val orientation = Resources.getSystem().configuration.orientation
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        Log.d("ImageCropDebug1", "device--${width}--${height}")
        val top: Float
        val bottom: Float
        val left: Float
        val right: Float
        val arcFactor = 16f
        val marginFactor = 100f
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val centerWidth = width / 2
            val centerHeight = (height - (2 * marginFactor)) / 2
            top = marginFactor
            bottom = (height - marginFactor)
            left = (centerWidth - centerHeight)
            right = (centerWidth + centerHeight)
        } else {
            val centerWidth = (width - (marginFactor * 2)) / 2
            val centerHeight = height / 2
            top = (centerHeight - centerWidth)
            bottom = (centerHeight + centerWidth)
            left = marginFactor
            right = (width - marginFactor)
        }

        Log.d("ImageCropDebug1", "${right-left}--${bottom-top}")
        mRect.left = left
        mRect.top = top
        mRect.right = right
        mRect.bottom = bottom
        canvas?.drawLine(left + arcFactor, top, right - arcFactor, top, paint)
        canvas?.drawArc(left, top, left + 2 * arcFactor, top + 2 * arcFactor, 180f, 90f, false, paint)
        canvas?.drawLine(right, top + arcFactor, right, bottom - arcFactor, paint)
        canvas?.drawArc(right - 2 * arcFactor, top, right, top + 2 * arcFactor, 270f, 90f, false, paint)
        canvas?.drawLine(right - arcFactor, bottom, left + arcFactor, bottom, paint)
        canvas?.drawArc(right - 2 * arcFactor, bottom - 2 * arcFactor, right, bottom, 0f, 90f, false, paint)
        canvas?.drawLine(left, bottom - arcFactor, left, top + arcFactor, paint)
        canvas?.drawArc(left , bottom - 2 * arcFactor, left + 2 * arcFactor, bottom, 90f, 90f, false, paint)
    }

    fun getRect() = mRect

    constructor(context: Context?) : super(context) {
//        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
//        init()
    }

}