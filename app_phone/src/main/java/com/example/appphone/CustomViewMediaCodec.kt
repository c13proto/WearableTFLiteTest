package com.example.appphone

import android.content.Context
import android.graphics.*
import android.media.*
import android.opengl.GLSurfaceView
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Switch
import com.example.app_phone.R
import com.example.common.CvUtils
import com.example.common.CvUtils.convertYuvToBitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList


class CustomViewMediaCodec @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr)
{

    companion object {//コールバック関係
        var onStop:(() -> Unit)? = null
        var onFrameChange: ((yuvBuffer:ByteArray,yuvType:Int,width:Int,height:Int,pitch:Int) -> Unit)?=null
//        var onFrameChangeBitmap:((bitmap:Bitmap)->Unit)?=null
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
    private val classifierResult=ArrayList<String>()

    private val mlock = Any()
    private var lastSeekPosition = 0L



    private lateinit var mSeekbar: SeekBar
    private lateinit var mSwitch: Switch
    private var preOnDraw=0L
    private var mEnableOverlayDebug=false//ViewのonDrawで描画させるか
    private lateinit var mGLSurfaceView: GLSurfaceView
    private var mGPUImage:GPUImage?=null

    fun setupCustomViewMediaCodec(seekBar: SeekBar,switch: Switch,gl_preview:GLSurfaceView,enableOverlayDebug:Boolean){//extensionではnullになったのでmainActivityから渡している
        Log.d("yama","setupCustomViewMediaCodec")
        mHandler=Handler()

        mPaintRect.style = Paint.Style.STROKE

        mPaintText.color=Color.GREEN
        mPaintText.textSize=textSize
        mEnableOverlayDebug=enableOverlayDebug
        mGPUImage = GPUImage(context)
        mGPUImage!!.setGLSurfaceView(gl_preview)
        mGLSurfaceView=gl_preview
        mSeekbar=seekBar
        mSwitch=switch

        setupLayout()
        setupClassifierCallback()

        mSwitch.setOnCheckedChangeListener({ _, isChecked ->
            // do something, the isChecked will be
            // true if the switch is in the On position
            Log.d("yama","OnCheckedChangeListener")
            if(isChecked)start()
            else pause()
        })

    }

    private fun setupClassifierCallback(){
        ImageClassifierTest.onClassifierResultCallback ={
            synchronized(mlock) {
                classifierResult.clear()
                for (row: String in it) {
                    classifierResult.add(row)
                }
            }
        }
    }


    private fun setupLayout() {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.defaultDisplay.getSize(mDisplaySize)

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(VideoAFD.fileDescriptor,VideoAFD.startOffset,VideoAFD.length)
//        retriever.setDataSource(context,Uri.parse(videoPath))
        mVideoeSize.x = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        mVideoeSize.y = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        retriever.release()
        calculateDrawScale()
        setLayoutMargin()
        setVideoPath()


    }
    private fun setLayoutMargin(){
        val margin=(mDisplaySize.y-mDisplaySize.x* mVideoeSize.y/ mVideoeSize.x)/2
        (mGLSurfaceView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0,margin,0,margin)
        (this.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, margin,0,margin)
    }

    private fun calculateDrawScale() {//ちょっと重そうなので一回だけ処理するようにする

        val scaleX = mDisplaySize.x.toFloat() / mVideoeSize.x
        val scaleY = mDisplaySize.y.toFloat() / mVideoeSize.y
        var  scale =    if (scaleX<scaleY) scaleX
                        else scaleY//画面枠ぴったりになる
        mDrawScale =scale
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
    private var preFrameCaptrueTime=0L
    internal fun onFrameCaptrueCtrl(yuvBuffer: ByteArray,yuv_type:Int ,width: Int, height: Int, pitch: Int) {//
//        val currentTime=System.currentTimeMillis()
//        Log.d("yama onFrameCaptrueCtrl","FPS=${1000f/(currentTime-preFrameCaptrueTime)}")
//        preFrameCaptrueTime=currentTime
        onFrameChange?.invoke(yuvBuffer, yuv_type,width, height, pitch)//コールバック


        if(mEnableOverlayDebug)
        {
            mHandler!!.removeCallbacksAndMessages(null)//非同期にすると再生時間がおかしくなるかも
            mHandler!!.post({
//                updateFrame(nv12buffer, width, height, pitch)
                mGPUImage!!.setImage(convertYuvToBitmap(yuvBuffer,yuv_type, width, height, pitch))
                invalidate()
                mSeekbar.progress = (getCurrentPosition() / 1000).toInt()
            })
        }

//        if(!didWriteBuffer){
//            outputByteArray(yuvBuffer)
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
        canvas.scale(mDrawScale, mDrawScale)
        if (mFrame != null)drawFrame(canvas)
        drawClassifer(canvas)

    }
    private fun drawFrame(canvas: Canvas){

        canvas.drawBitmap(mFrame, 0f, 0f, mPaintRect)
    }

    private fun drawClassifer(canvas: Canvas){
        var offset=0f
        synchronized(mlock) {
            for (row: String in classifierResult) {
                canvas.drawText(row, mDrawOffset.x.toFloat(), mDrawOffset.y.toFloat() + 20f + offset, mPaintText)
                offset += textSize
            }
        }

    }

    internal fun updateFrame(nv12buffer: ByteArray, width: Int, height: Int, pitch: Int) {
        synchronized(mlock) {
            mFrame?.recycle()
            mFrame = convertYuvToBitmap(nv12buffer, CvUtils.YUV_NV12, width, height, pitch)
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
        private var videoStride:Int=0
        private var videoPitch:Int=0
        private var videoYuvType=0


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
            while (!interrupted() && isPlaying) {
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
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->{
                        Log.d("DecodeActivity", "New format " + mDecoder.outputFormat)
                        getVideoInfo()
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d("DecodeActivity", "dequeueOutputBuffer timed out!")
                    else -> {
                        val buffer = mDecoder.getOutputBuffer(outIndex)
                        val byteArray = ByteArray(buffer!!.remaining())
                        buffer.get(byteArray)
                        if (byteArray.size >= videoHeight.toDouble() * videoWidth.toDouble() * 1.5) {//nv12のサイズになっているとき(ずれてるときもある)
                            mCustomViewMediaCodec.onFrameCaptrueCtrl(byteArray,videoYuvType, videoWidth, videoHeight, videoPitch)//高さを16の倍数にする
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
                                sleep(5)
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

        private fun getVideoInfo() {
            videoWidth = mDecoder.outputFormat.getInteger(MediaFormat.KEY_WIDTH)
            videoHeight = mDecoder.outputFormat.getInteger(MediaFormat.KEY_HEIGHT)
            videoPitch = mDecoder.outputFormat.getInteger(MediaFormat.KEY_SLICE_HEIGHT)
            videoStride = mDecoder.outputFormat.getInteger(MediaFormat.KEY_STRIDE)
            when (mDecoder.outputFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT)) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> videoYuvType = CvUtils.YUV_NV12
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> videoYuvType = CvUtils.YUV_I420
                else -> videoYuvType = CvUtils.YUV_NV21
            }
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