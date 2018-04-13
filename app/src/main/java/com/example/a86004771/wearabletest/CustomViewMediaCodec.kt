package com.example.a86004771.wearabletest

import android.content.Context
import android.graphics.*
import android.media.*
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import com.sonymobile.agent.robot.camera.CvUtils
import com.sonymobile.agent.robot.camera.CvUtils.convertI420ToBitmap
import com.sonymobile.agent.robot.camera.DetectedObject
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList


class CustomViewMediaCodec @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr)
{

    companion object {//コールバック関係
        var onStop:(() -> Unit)? = null
        var onFrameChange: ((i420Buffer:ByteArray ,width:Int,height:Int,pitch:Int) -> Unit)?=null
        var onFrameChangeBitmap:((bitmap:Bitmap)->Unit)?=null

        val mVideoeSize=Point()
        val mDisplaySize=Point()
        val mDrawOffset=Point()
        var mDrawScale=1f
    }


//    private var VideoPath: String="android.resource://" + context.packageName + "/raw/" + R.raw.qvga
    private val VideoAFD=resources.openRawResourceFd(R.raw.qvga)
    private val TAG = "CustomViewMediaCodec"
    private var mPlayer: PlayerThread? = null
    private var mHandler: Handler?=null//Thread中でSeekBar扱うため
//    private var isVideoFileExist = false

    private var mFrame: Bitmap?=null
    private val mPaintRect=Paint()
    private val mPaintText=Paint()
    private val textSize=25f
    val detectedObjects = ArrayList<DetectedObject>()
    val classifierResult=ArrayList<String>()

    private val mlock = Any()
    private var lastSeekPosition = 0L



    private lateinit var mSeekbar: SeekBar
    private lateinit var mSwitch: Switch
    private var preOnDraw=0L

    fun setupCustomViewMediaCodec(seekBar: SeekBar,switch: Switch){//extensionではnullになったのでmainActivityから渡している
        Log.d("yama","setupCustomViewMediaCodec")
        mHandler=Handler()

        mPaintRect.style = Paint.Style.STROKE

        mPaintText.color=Color.GREEN
        mPaintText.textSize=textSize

        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.defaultDisplay.getSize(mDisplaySize)

        mSeekbar=seekBar
        mSwitch=switch

        setupTestVideo()
        setupDetectorCallback()
        setupClassifierCallback()

        mSwitch.setOnCheckedChangeListener({ _, isChecked ->
            // do something, the isChecked will be
            // true if the switch is in the On position
            Log.d("yama","OnCheckedChangeListener")
            if(isChecked)start()
            else pause()
        })

    }
    private fun setupDetectorCallback(){
        DetectorTest.onDetectorResultCallback={
            detectedObjects.clear()
            for(obj: DetectedObject in it){
                detectedObjects.add(obj)
            }
        }
    }
    private fun setupClassifierCallback(){
        ImageClassifierTest.onClassifierResultCallback={
            classifierResult.clear()
            for(row:String in it){
                classifierResult.add(row)
            }
        }
    }


    private fun setupTestVideo() {
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(VideoAFD.fileDescriptor,VideoAFD.startOffset,VideoAFD.length)
//        retriever.setDataSource(context,Uri.parse(videoPath))
        mVideoeSize.x = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        mVideoeSize.y = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        retriever.release()
        calculateDrawScale()
        setVideoPath()


    }

    private fun calculateDrawScale() {//ちょっと重そうなので一回だけ処理するようにする

        val scaleX = mDisplaySize.x.toFloat() / mVideoeSize.x
        val scaleY = mDisplaySize.y.toFloat() / mVideoeSize.y
        var scale = if (java.lang.Float.compare(scaleX, scaleY) < 1) scaleX else scaleY//画面枠ぴったりになる
        //丸型なのでさらに縮める
        val theta = Math.atan(mVideoeSize.y.toDouble() / mVideoeSize.x)
        if (mVideoeSize.x > mVideoeSize.y) scale *= Math.cos(theta).toFloat()
        else scale *= Math.sin(theta).toFloat()

        mDrawScale=scale
        mDrawOffset.x= (((mDisplaySize.x.toFloat() - mVideoeSize.x * scale) / 2)/scale).toInt()//offsetもcaleに合わせる
        mDrawOffset.y= (((mDisplaySize.y.toFloat() - mVideoeSize.y * scale) / 2)/scale).toInt()
        Log.d("yama calculateDrawScale", "scale=$mDrawScale")
        Log.d("yama calculateDrawScale","display="+mDisplaySize.x+","+mDisplaySize.y)
        Log.d("yama calculateDrawScale","video="+mVideoeSize.x+","+mVideoeSize.y)
        Log.d("yama calculateDrawScale","offset="+mDrawOffset.x+","+mDrawOffset.y)
    }

    fun start() {


        if (mPlayer is PlayerThread) {
            mPlayer!!.interrupt()
            mPlayer = null
        }
        mPlayer = PlayerThread(this)
        mPlayer!!.start()

    }

    fun getCurrentPosition(): Long {
        if(mPlayer is PlayerThread)return mPlayer!!.getCurrentVideoPosition()
        else return 0
    }
    fun pause() {
        lastSeekPosition = getCurrentPosition()
        mPlayer?.pause()
        mPlayer?.interrupt()
        onStop?.invoke()
    }

    private fun setVideoPath() {
        mPlayer?.pause()
//        VideoPath = videoPath
        lastSeekPosition = 0
    }

private var didWriteBuffer=false
    internal fun onFrameCaptrueCtrl(i420buffer: ByteArray, width: Int, height: Int, pitch: Int) {//
//        Log.d("yama","width,height,pitch="+width+","+height+","+pitch)//ログ出すと激重
        onFrameChange?.invoke(i420buffer, width, height, pitch)//コールバック

//        mHandler!!.removeCallbacksAndMessages(null)//非同期にすると再生時間がおかしくなるかも
//        mHandler!!.post({
//            updateFrame(i420buffer, width, height, pitch)
//            invalidate()
//            mSeekbar.progress = (getCurrentPosition() / 1000).toInt()
//        })

//        if(!didWriteBuffer){
//            outputByteArray(i420buffer)
//            didWriteBuffer=true
//        }

    }

    private fun outputByteArray(img: ByteArray) {

        try {
            val outputStream = FileOutputStream("/sdcard/Raw")
            outputStream.write(img)
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.e("yama detectObject", e.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("yama detectObject", e.toString())
        }

    }

    public override fun onDraw(canvas: Canvas) {//Overlayの仕様に合わせている

//        Log.d("yama","onDrawFPS=${1000f/(System.currentTimeMillis()-preOnDraw)}")
//        val FPS=1000f/(System.currentTimeMillis()-preOnDraw)
//        preOnDraw=System.currentTimeMillis()
        if (mFrame != null) {
            drawFrame(canvas)
            //            canvas.drawText(FPS.toString(),mDrawOffset.x.toFloat(),mDrawOffset.y.toFloat()+20f,mPaint)
            drawRect(canvas)
            drawClassifer(canvas)
        }

    }
    private fun drawFrame(canvas: Canvas){
        canvas.scale(mDrawScale, mDrawScale)
        canvas.drawBitmap(mFrame,mDrawOffset.x.toFloat(),mDrawOffset.y.toFloat(), mPaintRect)
    }
    private fun drawRect(canvas: Canvas){

        for(obj:DetectedObject in detectedObjects){

            when(obj.name()){
                "searching"->mPaintRect.color=Color.RED
                "motion"->mPaintRect.color=Color.BLUE
                else->mPaintRect.color=Color.YELLOW
            }
            val drawRect=Rect(obj.xPosition(),obj.yPosition(),obj.xPosition()+obj.width(),obj.yPosition()+obj.height())
            drawRect.offset(mDrawOffset.x, mDrawOffset.y)
            canvas.drawRect(drawRect,mPaintRect)
        }
    }
    private fun drawClassifer(canvas: Canvas){
        var offset=0f
        for(row:String in classifierResult){
            canvas.drawText(row,mDrawOffset.x.toFloat(),mDrawOffset.y.toFloat()+20f+offset,mPaintText)
            offset+=textSize
        }

    }

    internal fun updateFrame(i420buffer: ByteArray, width: Int, height: Int, pitch: Int) {
        synchronized(mlock) {
            mFrame?.recycle()
            mFrame = convertI420ToBitmap(i420buffer, width, height, pitch)
//            CvUtils().yuvToBitmap(i420buffer, CvUtils.YUV_I420,width,height,pitch,mFrame!!)
//            onFrameChangeBitmap?.invoke(mFrame!!)
        }

    }


    inner class PlayerThread(var mCustomViewMediaCodec: CustomViewMediaCodec) : Thread() {
        var lastOffset: Long = 0
        var isPlaying = false
        private lateinit var mExtractor: MediaExtractor
        private lateinit var mDecoder: MediaCodec
        private var mBufferInfo: MediaCodec.BufferInfo?=null
        var videoDuration: Long = 0
        private val audioTrack: AudioTrack? = null
        private var videoWidth: Int = 0
        private var videoHeight: Int = 0

        private var lastPresentationTimeUs: Long = 0
        private var seeked = false
        private var startMs: Long = 0
        private var diff: Long = 0
        private var lastCorrectPresentationTimeUs: Long = 0

        val currentPosition: Int
            get() = if (mBufferInfo != null)
                (mBufferInfo!!.presentationTimeUs / 1000).toInt()
            else
                0


        override fun run() {
            try {
                MediaCodecPlay()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        @Throws(IOException::class)
        private fun MediaCodecPlay() {
            mExtractor = MediaExtractor()
            mExtractor.setDataSource(VideoAFD.fileDescriptor,VideoAFD.startOffset,VideoAFD.length)

            var format: MediaFormat
            for (i in 0 until mExtractor.trackCount) {
                format = mExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                videoDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000

                Log.d(TAG, "MIME: $mime")
                Log.d(TAG, "videoDuration=$videoDuration")

                if (mime.startsWith("video/")) {
                    mExtractor.selectTrack(i)
                    mDecoder = MediaCodec.createDecoderByType(mime)
                    mDecoder.configure(format, null, null, 0)
                    videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
                    videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
                    break
                }
            }

            mDecoder.start()
            mBufferInfo = MediaCodec.BufferInfo()
            startMs = System.currentTimeMillis()

            if (lastSeekPosition != 0L) seekTo(lastSeekPosition)
            setupSeekbar()


            var isEOS = false
            isPlaying = true
            while (!Thread.interrupted() && isPlaying) {
                if (!isEOS) {
                    val inIndex = mDecoder.dequeueInputBuffer(1000)
                    if (inIndex >= 0) {
                        val buffer = mDecoder.getInputBuffer(inIndex)

                        val sampleSize = mExtractor.readSampleData(buffer!!, 0)
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to mDecoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                            mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
//                            Log.d(TAG, "Queue Input Buffer at position: " + mBufferInfo!!.presentationTimeUs)
                            mDecoder.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.sampleTime, 0)
                            mExtractor.advance()
                        }

                    }
                }

                val outIndex = mDecoder.dequeueOutputBuffer(mBufferInfo!!, 1000)

                if (mBufferInfo!!.presentationTimeUs < lastPresentationTimeUs) {      // correct timing playback issue for some videos
                    startMs = System.currentTimeMillis()
                    lastCorrectPresentationTimeUs = lastPresentationTimeUs
                }


                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED")
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.d("DecodeActivity", "New format " + mDecoder.outputFormat)
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d("DecodeActivity", "dequeueOutputBuffer timed out!")
                    else -> {
                        val buffer = mDecoder.getOutputBuffer(outIndex)
                        val byteArray = ByteArray(buffer!!.remaining())
                        buffer.get(byteArray)
                        if (byteArray.size >= videoHeight.toDouble() * videoWidth.toDouble() * 1.5) {//nv12のサイズになっているとき(ずれてるときもある)
                            mCustomViewMediaCodec.onFrameCaptrueCtrl(byteArray, videoWidth, videoHeight, videoHeight + videoHeight.and(15))//高さを16の倍数にする
                        }
                        //Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        //                        We use a very simple clock to keep the video FPS, or the video
                        //                        playback will be too fast

//                        Log.d(TAG, "Original Presentation time: " + mBufferInfo!!.presentationTimeUs / 1000 + ", Diff PT: " + (mBufferInfo!!.presentationTimeUs / 1000 - lastOffset) + " : System Time: " + (System.currentTimeMillis() - startMs))

                        lastPresentationTimeUs = mBufferInfo!!.presentationTimeUs

                        if (seeked && Math.abs(mBufferInfo!!.presentationTimeUs / 1000 - lastOffset) < 100)
                            seeked = false

                        while (!seeked && mBufferInfo!!.presentationTimeUs / 1000 - lastOffset > System.currentTimeMillis() - startMs) {
                            try {
                                Thread.sleep(5)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                                break
                            }

                        }
                        mDecoder.releaseOutputBuffer(outIndex, false)
                    }
                }

                // All decoded frames have been rendered, we can stop playing now
                if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    break
                }
            }
            isPlaying = false
            mDecoder.stop()

            //再生終了しても保持しておきたい
            //            mDecoder.release();
            //            mExtractor.release();
        }

        fun getCurrentVideoPosition(): Long {
            if (mBufferInfo != null)
                return (mBufferInfo!!.presentationTimeUs / 1000)
            else
                return 0
        }
        fun seekTo(i: Long) {
            seeked = true
            Log.d(TAG, "SeekTo Requested to : $i")
            Log.d(TAG, "SampleTime Before SeekTo : " + mExtractor.sampleTime / 1000)
            mExtractor.seekTo(i * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            Log.d(TAG, "SampleTime After SeekTo : " + mExtractor.sampleTime / 1000)

            lastOffset = mExtractor.sampleTime / 1000
            startMs = System.currentTimeMillis()
            diff = lastOffset - lastPresentationTimeUs / 1000

            Log.d(TAG, "SeekTo with diff : $diff")
        }

        fun pause() {
            isPlaying = false
        }

        fun play() {
            run()
        }

        fun setupSeekbar() {
            mHandler?.post({
                mSeekbar.max = (videoDuration / 1000).toInt()//Durationはms
                mSeekbar.progress = (lastSeekPosition / 1000).toInt()
                mSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            seekTo((progress * 1000).toLong())
                            lastSeekPosition = (progress * 1000).toLong()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            })
        }


    }


}