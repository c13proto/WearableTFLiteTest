package com.example.a86004771.wearabletest

import android.content.Context
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import com.sonymobile.generalobjectdetector.GeneralObjectAssetsUnpacker
import com.sonymobile.generalobjectdetector.GeneralObjectDetector
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

class DetectorTest{
    private val TAG = "TrackerTest"
    private lateinit var  mContext: Context
    private val INIT_DATA_DIRECTORY_NAME = "init_data"
    private val INIT_DATA_FILE_NAME = "init_data"

    private val detectionImageWidth = 288
    private val detectionImageHeight = 288

    private var mDetector: GeneralObjectDetector? = null
    private var mDetectorBuffer: ByteBuffer? = null//rgb

    private var mCallerHandler: Handler? = null//mainにコールバックするやつ
    private var mDetectionThread: HandlerThread? = null
    private var mDetectionHandler: Handler? = null

    private val mlock = Any()
    private var isDetectionReady = false

    companion object {
        var onDetectorResultCallback: ((RectF) -> Unit)?=null
    }
    constructor(context:Context){
        mContext = context
        setupGeneralObjectDetector()
        setupDetectingTask()
    }
    private fun setupGeneralObjectDetector(){
        if (mDetector ==null) {
            val unpacker = GeneralObjectAssetsUnpacker(mContext)
            val sodFile: File
            try {
                sodFile = unpacker.unpackSodFile()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            val initData = File(mContext.getDir(INIT_DATA_DIRECTORY_NAME, Context.MODE_PRIVATE), INIT_DATA_FILE_NAME)
            mDetector = GeneralObjectDetector()
            mDetector!!.initialize(sodFile, initData)
        }
    }

    private fun setupDetectingTask(){

        CustomViewMediaCodec.onFrameChange={nv12Buffer,width,height,pitch ->
            Log.d("yama setupDetectingTask","width=${width}")

        }

        CustomViewMediaCodec.onStop={
            Log.d("yama setupDetectingTask","onStop")
        }

    }

    private fun startHandler() {
        synchronized(mlock) {
            if (mCallerHandler == null)
                mCallerHandler = Handler(Looper.getMainLooper())//mainにコールバックするやつ
            if (mDetectionThread == null) {//ObjectTrackerが継承しているDetectorに習ってスレッド作成
                mDetectionThread = HandlerThread(this.javaClass.simpleName + " Thread", Process.THREAD_PRIORITY_DISPLAY)
                mDetectionThread!!.start()
            }
            if (mDetectionHandler == null) {
                mDetectionHandler = Handler(mDetectionThread!!.getLooper())
            }
        }
    }

    private fun stopHandler() {
        synchronized(mlock) {
            if (mDetectionHandler != null) {
                mDetectionHandler!!.removeCallbacksAndMessages(null)
                mDetectionHandler = null
            }
            if (mDetectionThread != null) {
                mDetectionThread!!.quitSafely()
                try {
                    mDetectionThread!!.join()
                } catch (interrupted: InterruptedException) {
                    // Do nothing
                }
                mDetectionThread = null
            }
        }
    }



}