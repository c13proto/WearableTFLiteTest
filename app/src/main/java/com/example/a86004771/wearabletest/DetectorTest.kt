package com.example.a86004771.wearabletest

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.sonymobile.agent.robot.camera.CvUtils
import com.sonymobile.agent.robot.camera.DetectedObject
import com.sonymobile.generalobjectdetector.GeneralDetectedObject
import com.sonymobile.generalobjectdetector.GeneralObjectAssetsUnpacker
import com.sonymobile.generalobjectdetector.GeneralObjectDetector
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayList

class DetectorTest {
    private val TAG = "TrackerTest"
    private lateinit var mContext: Context
    private val INIT_DATA_DIRECTORY_NAME = "init_data"
    private val INIT_DATA_FILE_NAME = "init_data"
    private val detectionImageWidth = 640
    private val detectionImageHeight = 480

    private var mDetector: GeneralObjectDetector? = null
    private var mDetectorBuffer: ByteBuffer? = null//rgb

    private var mDetectionThread: HandlerThread? = null
    private var mDetectionHandler: Handler? = null

    private val mlock = Any()
    private var isDetectionReady = false

    companion object {
        var onDetectorResultCallback: ((List<Rect>) -> Unit)? = null
    }

    constructor(context: Context) {
        mContext = context
        setupGeneralObjectDetector()
        setupDetectingTask()
    }


    private fun setupGeneralObjectDetector() {
        if (mDetector == null) {
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

    private fun setupDetectingTask() {

        CustomViewMediaCodec.onFrameChange = { i420Buffer, width, height, pitch ->
            //            Log.d("yama setupDetectingTask","width=${width}")
            if (!isDetectionReady) {
                allocateWorkingBuffer()
                startHandler()
                isDetectionReady = true
            }
            if (mDetectionHandler != null) {
                synchronized(mlock) {
                    mDetectionHandler!!.removeCallbacksAndMessages(null)
                    mDetectionHandler!!.post(ObjectDetectionTask(i420Buffer,width,height,pitch))
                }
            }

        }

        CustomViewMediaCodec.onStop = {
            Log.d("yama setupDetectingTask", "onStop")
            mDetectionHandler?.removeCallbacksAndMessages(null)
            isDetectionReady = false
        }

    }

    private fun allocateWorkingBuffer() {
        mDetectorBuffer = null
        mDetectorBuffer = ByteBuffer.allocateDirect(detectionImageWidth * detectionImageHeight * 3)
    }

    private fun startHandler() {
        synchronized(mlock) {

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

    private var outdebug_count=0
    inner class ObjectDetectionTask(yuvBuffer: ByteArray,width:Int,height:Int,pitch:Int) : Runnable
    {
        val orgI420Bytearray:ByteArray = yuvBuffer
        val orgImageWidth=width
        val orgImageHeight=height
        val orgImagePitch=pitch

        override fun run() {
            if (!CvUtils().yuvToRgb(orgI420Bytearray, CvUtils.YUV_I420, orgImageWidth, orgImageHeight, orgImagePitch, mDetectorBuffer!!, detectionImageWidth, detectionImageHeight, 3)) {
                Log.e(TAG, "Failed to create image nativeImageCrop")
                return
            }
            val objects = detectObject(mDetectorBuffer!!)
            invokeDetectorResult(objects)
        }

        private fun detectObject(rgb_img: ByteBuffer): List<DetectedObject> {
//                        if(outdebug_count==0){
//                            outputBytebuffer(rgb_img)
//                            outdebug_count++
//                        }
            val t1 = System.currentTimeMillis()
            val result: List<GeneralDetectedObject> = mDetector!!.detect(rgb_img, detectionImageWidth, detectionImageHeight)
            Log.d("yama", "detectObject:" + (System.currentTimeMillis() - t1) + "ms")
            val objects=ArrayList<DetectedObject>()
            for (generalDetectedObject:GeneralDetectedObject in result) {
                val detectedObject=DetectedObject(generalDetectedObject,detectionImageWidth,detectionImageHeight)
                resizeDetectedObject(detectedObject,orgImageWidth.toFloat()/detectionImageWidth,orgImageHeight.toFloat()/detectionImageHeight)
                objects.add(detectedObject)
                Log.d("yama","found ${detectedObject.name()}")
            }
            return objects
        }


    }

    private fun outputBytebuffer(img: ByteBuffer) {
        val byteArray = ByteArray(img.limit())
        img.get(byteArray)
        try {
            val outputStream = FileOutputStream("/sdcard/RgbRaw")
            outputStream.write(byteArray)
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.e("yama detectObject", e.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("yama detectObject", e.toString())
        }

    }
    private fun copyDetectedObject(dst: DetectedObject, src: DetectedObject, scaleX: Float, scaleY: Float) {
        dst.setX((src.xPosition() * scaleX).toInt())
        dst.setY((src.yPosition() * scaleY).toInt())
        dst.setWidth((src.width() * scaleX).toInt())
        dst.setHeight((src.height() * scaleY).toInt())
    }

    private fun resizeDetectedObject(obj: DetectedObject, scaleX: Float, scaleY: Float) {
        copyDetectedObject(obj, obj, scaleX, scaleY)
    }
    private fun invokeDetectorResult(objects:List<DetectedObject>){
        val rects = ArrayList<Rect>()
        for(obj:DetectedObject in objects){
            rects.add(detectedObjectToRect(obj))
        }
        onDetectorResultCallback?.invoke(rects)
    }

    private fun detectedObjectToRect(detectedObject: DetectedObject):Rect{
        return Rect(detectedObject.xPosition(),
                detectedObject.yPosition(),
                detectedObject.xPosition()+detectedObject.width(),
                detectedObject.yPosition()+detectedObject.height()
                )
    }
}