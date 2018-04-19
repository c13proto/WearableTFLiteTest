package com.example.appphone

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
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
    private val detectionImageWidth = 288
    private val detectionImageHeight = 288
    private val motionImageWidth = 80
    private val motionImageHeight =60

    private var mDetector: GeneralObjectDetector? = null
    private var mDetectorBuffer: ByteBuffer? = null//rgb
    private var mPreGrayBuffer: ByteBuffer? = null//gray motion検知のため
    private var mCallerHandler: Handler? = null
    private var mDetectionThread: HandlerThread? = null
    private var mDetectionHandler: Handler? = null

    private val mlock = Any()
    private var isDetectionReady = false


    private var mCropArea = intArrayOf(-100, -100, -100, -100)
    private var mMotionArea=intArrayOf(-100, -100, -100, -100)
    private fun disableMotionArea() { mMotionArea[3] = -100 }
    private fun isMotionAreaEnable(): Boolean { return mMotionArea[3] != -100}
    private fun isCropAreaEnable(): Boolean {return mCropArea[3] != -100}
    private var canMotionDetection=false
    private var mPreviousCropArea = CropArea.None
    protected enum class CropArea {
        None, LostROI, Original, Center, TopLeft, TopRight, BotLeft, BotRight, Motion, PreviousDetectionResult
    }


    companion object {
        var onDetectorResultCallback: ((List<DetectedObject>) -> Unit)? = null
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

        CustomViewMediaCodec.onFrameChange = { yuvBuffer, yuvType, width, height, pitch ->
            //            Log.d("yama setupDetectingTask","width=${width}")

            if (!isDetectionReady) {
                allocateWorkingBuffer()
                startHandler()
                isDetectionReady = true
            }
            if (mDetectionHandler != null) {
                synchronized(mlock) {
                    mDetectionHandler!!.removeCallbacksAndMessages(null)
                    mDetectionHandler!!.post(ObjectDetectionTask(yuvBuffer,yuvType,width,height,pitch))
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
        mPreGrayBuffer=null
        mDetectorBuffer = ByteBuffer.allocateDirect(detectionImageWidth * detectionImageHeight * 3)
        mPreGrayBuffer= ByteBuffer.allocateDirect(motionImageWidth*motionImageHeight)//DirectじゃないとNativeで扱えない
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

    private var outdebug_count=0
    inner class ObjectDetectionTask(yuvBuffer: ByteArray,yuvType:Int,width:Int,height:Int,pitch:Int) : Runnable
    {
        val orgYuvBytearray:ByteArray = yuvBuffer
        val orgImageYuvType=yuvType
        val orgImageWidth=width
        val orgImageHeight=height
        val orgImagePitch=pitch


        override fun run() {
            motionDetectCtrl()
            cropAreaCtrl(detectionImageHeight == detectionImageWidth)
            if (!CvUtils.yuvCropRotateToRgb(orgYuvBytearray, orgImageYuvType, orgImageWidth, orgImageHeight,orgImagePitch ,mCropArea,0, mDetectorBuffer!!, detectionImageWidth, detectionImageHeight, 3)) {
                Log.e(TAG, "Failed to create image nativeImageCrop")
                return
            }
            val objects = detectObject(mDetectorBuffer!!)
            setDebugArea(objects)
            onDetectorResultCallback?.invoke(objects)//TaskたまりすぎてUIスレッドだと要求が通らない？
//            mCallerHandler!!.post{
//                Runnable {onDetectorResultCallback?.invoke(objects) }
//
//            }

        }

        private fun setDebugArea(objects: ArrayList<DetectedObject>) {
            if (isCropAreaEnable()) {
                val search = DetectedObject(mCropArea[0], mCropArea[1], mCropArea[2], mCropArea[3])
                search.setName("searching")
                objects.add(search)
            }
            if (isMotionAreaEnable()) {
                val motion = DetectedObject(mMotionArea[0], mMotionArea[1], mMotionArea[2], mMotionArea[3])
                motion.setName("motion")
                objects.add(motion)
            }

            //            DetectedObject test=new DetectedObject(0,0,orgImageWidth/2,orgImageHeight/2);
            //            test.setName("test");
            //            objects.add(test);
        }

        private fun detectObject(rgb_img: ByteBuffer): ArrayList<DetectedObject> {
//                        if(outdebug_count<=100){
//                            if(outdebug_count==99)outputBytebuffer(rgb_img,"/sdcard/rgbRaw"+outdebug_count)
//                            outdebug_count++
//                        }
//            val t1 = System.currentTimeMillis()
            val result: List<GeneralDetectedObject> = mDetector!!.detect(rgb_img, detectionImageWidth, detectionImageHeight)
//            Log.d("yama detectObject","num of found :${result.size}")
//            Log.d("yama", "detectObject:" + (System.currentTimeMillis() - t1) + "ms")
            val objects=ArrayList<DetectedObject>()
            for (generalDetectedObject:GeneralDetectedObject in result) {
                val detectedObject=DetectedObject(generalDetectedObject,detectionImageWidth,detectionImageHeight)
                convertDetectionResultToAbsoluteScale(detectedObject)
//                resizeDetectedObject(detectedObject,orgImageWidth.toFloat()/detectionImageWidth,orgImageHeight.toFloat()/detectionImageHeight)

                objects.add(detectedObject)
                Log.d("yama","found ${detectedObject.name()}")
            }
            return objects
        }

        private fun convertDetectionResultToAbsoluteScale(obj: DetectedObject) {
            val search_x = 0
            val search_y = 0
            val search_w = orgImageWidth
            val search_h = orgImageHeight

            if (isCropAreaEnable()) {
                val crop_x = (mCropArea[0] - search_x).toFloat()//相対座標に変換
                val crop_y = (mCropArea[1] - search_y).toFloat()
                val crop_w = mCropArea[2].toFloat()
                val crop_h = mCropArea[3].toFloat()
                //                Log.d("yama_stop_check","isCropAreaEnable(x,y,w,h)=("+obj.xPosition()+","+obj.yPosition()+","+obj.width()+","+obj.height()+")");
                resizeDetectedObject(obj, crop_w / detectionImageWidth, crop_h / detectionImageHeight)
                obj.setX(obj.xPosition() + crop_x.toInt())
                obj.setY(obj.yPosition() + crop_y.toInt())
                //                Log.d("yama_stop_check","isCropAreaEnable(x,y,w,h)_=("+obj.xPosition()+","+obj.yPosition()+","+obj.width()+","+obj.height()+")");

            } else {
                //                Log.d("yama_stop_check","!isCropAreaEnable(x,y,w,h)=("+obj.xPosition()+","+obj.yPosition()+","+obj.width()+","+obj.height()+")");
                resizeDetectedObject(obj, search_w.toFloat() / detectionImageWidth, search_h.toFloat() / detectionImageHeight)
            }
//            copyDetectedObject(nextSetROI, obj, 1f, 1f)//結果をsetROIに渡す(SearchArea内相対座標)

            obj.setX(obj.xPosition() + search_x)
            obj.setY(obj.yPosition() + search_y)
        }

        private fun motionDetectCtrl() {
            val grayBuffer = ByteBuffer.allocateDirect(motionImageHeight * motionImageWidth)
            CvUtils.yuvToRgb(orgYuvBytearray, orgImageYuvType, orgImageWidth, orgImageHeight, orgImagePitch, grayBuffer, motionImageWidth, motionImageHeight, 1)

            if (canMotionDetection) {
//                val t1 = System.currentTimeMillis()
                val motionArea = intArrayOf(0, 0, 0, 0)

                if (CvUtils.getMotionArea(grayBuffer, mPreGrayBuffer!!, motionImageWidth, motionImageHeight, motionArea)) {
//                    Log.d("yama", "motionDetect:" + (System.currentTimeMillis() - t1) + "ms")
                    val scaleX=orgImageWidth.toFloat()/motionImageWidth
                    val scaleY=orgImageHeight.toFloat()/motionImageHeight
                    mMotionArea[0]=(motionArea[0]*scaleX).toInt()
                    mMotionArea[1]=(motionArea[1]*scaleY).toInt()
                    mMotionArea[2]=(motionArea[2]*scaleX).toInt()
                    mMotionArea[3]=(motionArea[3]*scaleY).toInt()
                }
                else {
                    disableMotionArea()
                }
            }
            mPreGrayBuffer!!.clear()
            mPreGrayBuffer!!.put(grayBuffer)
            mPreGrayBuffer!!.flip()
            canMotionDetection = true
        }



        private fun cropAreaCtrl(crop_square: Boolean) {


            if (isMotionAreaEnable()) {//中心から、全体領域の4分割した枠の大きさを探索させてみる
                custom_crop_ctrl(mMotionArea[0], mMotionArea[1], mMotionArea[2], mMotionArea[3], 1 / 2.0f, crop_square)
                mPreviousCropArea = CropArea.Motion
            }
//            else if (mPreviousCropArea == CropArea.Original) {
//                if (crop_square) setCropArea_Square(CropArea.Center) else setCropArea_Rect(CropArea.Center)
//            } else if (mPreviousCropArea == CropArea.Center) {
//                if (crop_square) setCropArea_Square(CropArea.TopLeft) else setCropArea_Rect(CropArea.TopLeft)
//            } else if (mPreviousCropArea == CropArea.TopLeft) {
//                if (crop_square) setCropArea_Square(CropArea.TopRight) else setCropArea_Rect(CropArea.TopRight)
//            } else if (mPreviousCropArea == CropArea.TopRight) {
//                if (crop_square) setCropArea_Square(CropArea.BotLeft) else setCropArea_Rect(CropArea.BotLeft)
//            } else if (mPreviousCropArea == CropArea.BotLeft) {
//                if (crop_square) setCropArea_Square(CropArea.BotRight) else setCropArea_Rect(CropArea.BotRight)
//            }
            else {
                if (crop_square) setCropArea_Square(CropArea.Original) else setCropArea_Rect(CropArea.Original)
            }
            //            Log.d("yama","State="+mPreviousCropArea+"mCropArea="+mCropArea[0]+","+mCropArea[1]+","+mCropArea[2]+","+mCropArea[3]);


        }

        private fun custom_crop_ctrl(x: Int, y: Int, w: Int, h: Int, scale: Float, square: Boolean) {
            //mPreciousResultかmMotionAreaが入ってくる(画像全体の絶対座標)
            val centerX: Int
            val centerY: Int
            var left: Int
            var top: Int
            var right: Int
            var bot: Int

            val search_x = 0
            val search_y = 0
            val search_w = orgImageWidth
            val search_h = orgImageHeight

            centerX = x + w / 2 - search_x//mSearchArea内での相対座標化
            centerY = y + h / 2 - search_y

            if (square) {
                var side = if (search_w < search_h) (search_w * scale).toInt() else (search_h * scale).toInt()
//                if(side<detectionImageWidth)side=detectionImageWidth
                left = (centerX - side / 2.0).toInt()
                top = (centerY - side / 2.0).toInt()
                right = (centerX + side / 2.0).toInt()
                bot = (centerY + side / 2.0).toInt()
                mCropArea[3] = side
                mCropArea[2] = mCropArea[3]//width,height
            } else {
                left = (centerX - search_w * scale / 2).toInt()
                top = (centerY - search_h * scale / 2).toInt()
                right = (left + search_w * scale).toInt()
                bot = (top + search_h * scale).toInt()
                mCropArea[2] = (search_w * scale).toInt()//width
                mCropArea[3] = (search_h * scale).toInt()//height
            }

            //領域を画面内に
            if (left < 0) {
                val diff = (0 - left).toFloat()
                left += diff.toInt()
                right += diff.toInt()
            }
            if (top < 0) {
                val diff = (0 - top).toFloat()
                top += diff.toInt()
                bot += diff.toInt()
            }
            if (right > search_w) {
                val diff = (right - search_w).toFloat()
                left -= diff.toInt()//right-=diff;
            }
            if (bot > search_h) {
                val diff = (bot - search_h).toFloat()
                top -= diff.toInt()//bot-=diff;
            }
            mCropArea[0] = left + search_x//x
            mCropArea[1] = top + search_y
        }

        private fun setCropArea_Rect(area: CropArea) {
            mPreviousCropArea = area
            val search_w = orgImageWidth
            val search_h = orgImageHeight
            if (area == CropArea.Original) {
                mCropArea[1] = 0
                mCropArea[0] = mCropArea[1]//y
                mCropArea[2] = search_w//width
                mCropArea[3] = search_h//height
            } else {
                val cropWidth = search_w / 2
                val cropHeight = search_h / 2

                if (area == CropArea.Center) {
                    mCropArea[0] = cropWidth / 2//x
                    mCropArea[1] = cropHeight / 2//y
                } else if (area == CropArea.TopLeft) {
                    mCropArea[1] = 0
                    mCropArea[0] = mCropArea[1]
                } else if (area == CropArea.TopRight) {
                    mCropArea[0] = cropWidth
                    mCropArea[1] = 0
                } else if (area == CropArea.BotLeft) {
                    mCropArea[0] = 0
                    mCropArea[1] = cropHeight
                } else if (area == CropArea.BotRight) {
                    mCropArea[0] = cropWidth
                    mCropArea[1] = cropHeight
                }

                mCropArea[2] = cropWidth//width
                mCropArea[3] = cropHeight//height
            }
//            mCropArea[0] += 0//サーチエリア
//            mCropArea[1] += 0

        }

        private fun setCropArea_Square(area: CropArea) {
            mPreviousCropArea = area
            val search_w = orgImageWidth
            val search_h = orgImageHeight
            if (search_h > search_w) {//Brightのカメラ映像のとき
                if (area == CropArea.Original) {
                    mCropArea[0] = 0
                    mCropArea[1] = search_h - search_w//y
                    mCropArea[3] = search_w
                    mCropArea[2] = mCropArea[3]//height
                } else {
                    val cropWidth = search_w / 2

                    if (area == CropArea.Center) {
                        mCropArea[0] = cropWidth / 2//x
                        mCropArea[1] = search_h - search_w + cropWidth / 2//y
                    } else if (area == CropArea.TopLeft) {
                        mCropArea[0] = 0
                        mCropArea[1] = search_h - search_w
                    } else if (area == CropArea.TopRight) {
                        mCropArea[0] = cropWidth
                        mCropArea[1] = search_h - search_w
                    } else if (area == CropArea.BotLeft) {
                        mCropArea[0] = 0
                        mCropArea[1] = search_h - cropWidth
                    } else if (area == CropArea.BotRight) {
                        mCropArea[0] = cropWidth
                        mCropArea[1] = search_h - cropWidth
                    }

                    mCropArea[3] = cropWidth
                    mCropArea[2] = mCropArea[3]//height
                }
            } else {
                if (area == CropArea.Original) {
                    mCropArea[0] = (search_w - search_h) / 2
                    mCropArea[1] = 0//y
                    mCropArea[3] = search_h
                    mCropArea[2] = mCropArea[3]//height
                } else {
                    val cropWidth = search_h / 2

                    if (area == CropArea.Center) {
                        mCropArea[0] = (search_w - cropWidth) / 2//x
                        mCropArea[1] = (search_h - cropWidth) / 2//y
                    } else if (area == CropArea.TopLeft) {
                        mCropArea[0] = 0
                        mCropArea[1] = 0
                    } else if (area == CropArea.TopRight) {
                        mCropArea[0] = search_w - cropWidth
                        mCropArea[1] = 0
                    } else if (area == CropArea.BotLeft) {
                        mCropArea[0] = 0
                        mCropArea[1] = search_h - cropWidth
                    } else if (area == CropArea.BotRight) {
                        mCropArea[0] = search_w - cropWidth
                        mCropArea[1] = search_h - cropWidth
                    }

                    mCropArea[3] = cropWidth
                    mCropArea[2] = mCropArea[3]//height
                }
            }
//            mCropArea[0] += 0
//            mCropArea[1] += 0
        }


    }

    private fun outputBytebuffer(img: ByteBuffer,path:String) {
        val byteArray = ByteArray(img.limit())
        img.get(byteArray)
        try {
            val outputStream = FileOutputStream(path)
            outputStream.write(byteArray)
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.e("yama outputBytebuffer", e.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("yama outputBytebuffer", e.toString())
        }

        Log.d("yama outputBytebuffer","output")

        img.clear()

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


    private fun detectedObjectToRect(detectedObject: DetectedObject):Rect{
        return Rect(detectedObject.xPosition(),
                detectedObject.yPosition(),
                detectedObject.xPosition()+detectedObject.width(),
                detectedObject.yPosition()+detectedObject.height()
        )
    }
}