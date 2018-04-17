package com.example.a86004771.wearabletest

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.sonymobile.agent.robot.camera.CvUtils
import com.sonymobile.agent.robot.camera.CvUtils.convertYuvToBitmap

public class ImageClassifierTest{

    var activity: Activity
    var classifier: ImageClassifier
    private val HANDLE_THREAD_NAME = "CameraBackground"

    private val lock = Any()
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    companion object {
        var onClassifierResultCallback: ((result:List<String>) -> Unit)? = null
    }

    constructor(activity: Activity)
    {
        this.activity=activity
        classifier= ImageClassifierQuantizedMobileNet(activity)
        startBackgroundThread()
        CustomViewMediaCodec.onFrameChange = { i420Buffer, width, height, pitch ->
            synchronized(lock){
                backgroundHandler.removeCallbacksAndMessages(null)
                backgroundHandler.post(periodicClassify(i420Buffer,width,height,pitch))
            }

        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(HANDLE_THREAD_NAME, Process.THREAD_PRIORITY_DISPLAY)
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    inner class periodicClassify(yuvBuffer: ByteArray,width:Int,height:Int,pitch:Int) : Runnable {
        val orgBytearray:ByteArray = yuvBuffer
        val orgImageWidth=width
        val orgImageHeight=height
        val orgImagePitch=pitch
        override fun run() {
            classifyFrame(orgBytearray,orgImageWidth,orgImageHeight,orgImagePitch)
        }
        private fun classifyFrame(yuvBuffer: ByteArray,width:Int,height:Int,pitch:Int) {

            val frameBitmap = convertYuvToBitmap(yuvBuffer,CvUtils.YUV_I420, width, height, pitch, classifier.imageSizeX, classifier.imageSizeY)
            val classifereResult = classifier.classifyFrame(frameBitmap)//textShowに内容が書かれているはず
            frameBitmap.recycle()//classfire.x classfire.yのサイズにする必要あり
            Log.d("yama classifyFrame",classifereResult)
            onClassifierResultCallback?.invoke(classifereResult.split("\n"))
        }
    }
}