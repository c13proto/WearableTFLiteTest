package com.example.appphone


import android.app.Activity
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.example.common.CvUtils
import com.example.common.ImageClassifier
import com.example.common.ImageClassifierQuantizedMobileNet

class ImageClassifierTest{

    var classifier: ImageClassifier
    private val HANDLE_THREAD_NAME = "TFLiteThread"

    //    private val lock = Any()
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private var preInputFrameTime=0L

    companion object {
        var onClassifierResultCallback: ((result:List<String>) -> Unit)? = null
    }

    constructor(activity: Activity)
    {
        classifier= ImageClassifierQuantizedMobileNet(activity)
        startBackgroundThread()
        CustomViewMediaCodec.onFrameChange = { yuvBuffer,yuvType, width, height, pitch ->
            val currentTime=System.currentTimeMillis()
            Log.d("yama ImageClassifiereTest","input FPS=${1000f/(currentTime-preInputFrameTime)}")
            preInputFrameTime=currentTime
//            synchronized(lock){
            backgroundHandler.removeCallbacksAndMessages(null)
            backgroundHandler.post(periodicClassify(yuvBuffer,yuvType,width,height,pitch))
//            }

        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(HANDLE_THREAD_NAME, Process.THREAD_PRIORITY_DISPLAY)
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    inner class periodicClassify(yuvBuffer: ByteArray,yuvType:Int,width:Int,height:Int,pitch:Int) : Runnable {
        private val orgBytearray:ByteArray = yuvBuffer
        private val orgYuvType=yuvType
        private val orgImageWidth=width
        private val orgImageHeight=height
        private val orgImagePitch=pitch
        override fun run() {
            classifyFrame()
        }
        private fun classifyFrame() {
            //bitmapをclassfire.x classfire.yのサイズにする必要あり
            val frameBitmap = CvUtils.convertYuvToBitmap(orgBytearray, orgYuvType, orgImageWidth, orgImageHeight, orgImagePitch, classifier.imageSizeX, classifier.imageSizeY)
            val classifereResult = classifier.classifyFrame(frameBitmap)//textShowに識別結果が書かれている
            frameBitmap.recycle()
            Log.d("yama classifyFrame",classifereResult)
            onClassifierResultCallback?.invoke(classifereResult.split("\n"))
        }
    }
}