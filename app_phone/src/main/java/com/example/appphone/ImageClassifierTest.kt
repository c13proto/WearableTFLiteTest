package com.example.appphone

import android.app.Activity
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.sonymobile.agent.robot.camera.CvUtils
import com.sonymobile.agent.robot.camera.CvUtils.convertYuvToBitmap

class ImageClassifierTest{

    var classifier: ImageClassifier
    private val HANDLE_THREAD_NAME = "CameraBackground"

//    private val lock = Any()
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    companion object {
        var onClassifierResultCallback: ((result:List<String>) -> Unit)? = null
    }

    constructor(activity: Activity)
    {
        classifier= ImageClassifierQuantizedMobileNet(activity)
        startBackgroundThread()
        CustomViewMediaCodec.onFrameChange = { nv12Buffer, width, height, pitch ->
//            synchronized(lock){
                backgroundHandler.removeCallbacksAndMessages(null)
                backgroundHandler.post(periodicClassify(nv12Buffer,width,height,pitch))
//            }

        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread(HANDLE_THREAD_NAME, Process.THREAD_PRIORITY_DISPLAY)
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    inner class periodicClassify(yuvBuffer: ByteArray,width:Int,height:Int,pitch:Int) : Runnable {
        private val orgNV21Bytearray:ByteArray = yuvBuffer
        private val orgImageWidth=width
        private val orgImageHeight=height
        private val orgImagePitch=pitch
        override fun run() {
            classifyFrame(orgNV21Bytearray,orgImageWidth,orgImageHeight,orgImagePitch)
        }
        private fun classifyFrame(yuvBuffer: ByteArray,width:Int,height:Int,pitch:Int) {

            val frameBitmap = convertYuvToBitmap(yuvBuffer,CvUtils.YUV_NV12, width, height, pitch, classifier.imageSizeX, classifier.imageSizeY)
            val classifereResult = classifier.classifyFrame(frameBitmap)
            frameBitmap.recycle()
            Log.d("yama classifyFrame",classifereResult)
            onClassifierResultCallback?.invoke(classifereResult.split("\n"))
        }
    }

}