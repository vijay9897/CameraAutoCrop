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
        paint.strokeWidth = 10f
        paint.style = Paint.Style.FILL
        /*path effect is used to set the dashed effect if full rectangle border is shown in frame*/
//        paint.pathEffect = pathEffect
        val displayMetrics = Resources.getSystem().displayMetrics
        val orientation = Resources.getSystem().configuration.orientation
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val top: Float
        val bottom: Float
        val left: Float
        val right: Float
        /*arcFactor is used to give rounded corner for the rectangle if no rounding experience is need set it to 0.*/
        val arcFactor = 0f
        /*marginFactor is used to give spacing to the frame from left and right. If no spacing is required set it to 0.*/
        val marginFactor = 0f
        /*cornerLineLength is used for setting the required length of the corner lines.*/
        val cornerLineLength = 40f

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

        /*setting the rectangle to use it later.*/
        mRect.left = left
        mRect.top = top
        mRect.right = right
        mRect.bottom = bottom

        /*top left corner*/
        canvas?.drawLine(left + arcFactor, top, left + arcFactor + cornerLineLength, top, paint)
        canvas?.drawLine(left + arcFactor, top, left + arcFactor, top + arcFactor + cornerLineLength, paint)
//        canvas?.drawArc(left, top, left + 2 * arcFactor, top + 2 * arcFactor, 180f, 90f, false, paint)
        /*top right corner*/
        canvas?.drawLine(right, top + arcFactor, right, top + arcFactor + cornerLineLength, paint)
        canvas?.drawLine(right, top + arcFactor, right - arcFactor - cornerLineLength, top + arcFactor, paint)
//        canvas?.drawArc(right - 2 * arcFactor, top, right, top + 2 * arcFactor, 270f, 90f, false, paint)
        /*bottom right corner*/
        canvas?.drawLine(right - arcFactor, bottom, right - arcFactor - cornerLineLength, bottom, paint)
        canvas?.drawLine(right - arcFactor, bottom - arcFactor, right - arcFactor, bottom - arcFactor - cornerLineLength, paint)
//        canvas?.drawArc(right - 2 * arcFactor, bottom - 2 * arcFactor, right, bottom, 0f, 90f, false, paint)
        /*bottom left corner*/
        canvas?.drawLine(left, bottom - arcFactor, left, bottom - arcFactor - cornerLineLength, paint)
        canvas?.drawLine(left + arcFactor, bottom, left + arcFactor + cornerLineLength, bottom, paint)
//        canvas?.drawArc(left , bottom - 2 * arcFactor, left + 2 * arcFactor, bottom, 90f, 90f, false, paint)

        /*rectangles to cover the remaining space with about 40% transparency*/
        /*bottom rectangle*/
        val bottomRect = Rect(0, bottom.toInt(), width, height)
        /*top rectangle*/
        val topRect = Rect(0, 0, width, top.toInt())
        /*left rectangle. This rectangle will not be visible if marginFactor is 0*/
        val leftRect = Rect(0, top.toInt(), left.toInt(), bottom.toInt())
        /*right rectangle. This rectangle will not be visible if marginFactor is 0*/
        val rightRect = Rect(right.toInt(), top.toInt(), width, bottom.toInt())
        paint.color = Color.parseColor("#B4000000")
        paint.style = Paint.Style.FILL
        canvas?.drawRect(bottomRect, paint)
        canvas?.drawRect(topRect, paint)
        canvas?.drawRect(leftRect, paint)
        canvas?.drawRect(rightRect, paint)
    }

    fun getRect() = mRect

    constructor(context: Context?) : super(context) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    }

}