package com.example.app_phone

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.sonymobile.agent.robot.camera.CvUtils

public class ImageClassifierTest{

    lateinit var activity: Activity
    lateinit var classifier: ImageClassifier
    private var frameBitmap:Bitmap?=null
    private val HANDLE_THREAD_NAME = "CameraBackground"

    private val lock = Any()
    private var runClassifier = false
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
            synchronized(lock) {
                frameBitmap?.recycle()//classfire.x classfire.yのサイズにする必要あり
                frameBitmap = convertI420ToBitmap(i420Buffer, width, height, pitch, classifier.imageSizeX, classifier.imageSizeY)
                runClassifier = true
            }
        }
    }
    private fun convertI420ToBitmap(i420buffer: ByteArray, width: Int, height: Int, pitch: Int,dstWidth:Int=width,dstHeight:Int=height): Bitmap {
        val bitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        CvUtils.yuvToBitmap(i420buffer,CvUtils.YUV_I420,width,height,pitch,bitmap,dstWidth,dstHeight)
        return bitmap
    }
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(HANDLE_THREAD_NAME)
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
        backgroundHandler.post(periodicClassify)
    }

    private val periodicClassify = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (runClassifier) {
                    runClassifier=false//次のfram更新まで実行させない
                    classifyFrame()

                }
            }
            backgroundHandler.post(this)
        }
    }
    private fun classifyFrame() {
        val textToShow = classifier.classifyFrame(frameBitmap)//textShowに内容が書かれているはず
        Log.d("yama classifyFrame",textToShow)
        onClassifierResultCallback?.invoke(textToShow.split("\n"))
    }
}