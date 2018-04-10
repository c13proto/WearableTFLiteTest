package com.example.a86004771.wearabletest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.example.a86004771.wearabletest.CustomViewMediaCodec.Companion.mDrawOffset
import com.sonymobile.agent.robot.camera.DetectedObject
import java.util.*

class OverlayView @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr)
{
    val detectedObjects =ArrayList<DetectedObject>()
    var videoWidth=0
    var videoHeight=0
    val mPaint= Paint()

    init {

        mPaint.style = Paint.Style.STROKE

        DetectorTest.onDetectorResultCallback={
            detectedObjects.clear()
            for(obj:DetectedObject in it){
                detectedObjects.add(obj)
            }
            invalidate()
        }

    }

    override fun onDraw(canvas: Canvas?) {

        canvas?.scale(CustomViewMediaCodec.mDrawScale, CustomViewMediaCodec.mDrawScale)
        for(obj:DetectedObject in detectedObjects){

            when(obj.name()){
                "searching"->mPaint.color=Color.RED
                "motion"->mPaint.color=Color.BLUE
                else->mPaint.color=Color.YELLOW
            }

            val drawRect=Rect(obj.xPosition(),obj.yPosition(),obj.xPosition()+obj.width(),obj.yPosition()+obj.height())
            drawRect.offset(mDrawOffset.x, mDrawOffset.y)
            canvas?.drawRect(drawRect,mPaint)
        }


//            canvas?.drawBitmap(mFrame, CustomViewMediaCodec.mDrawOffset.x.toFloat(), CustomViewMediaCodec.mDrawOffset.y.toFloat(), mPaint)
    }


}