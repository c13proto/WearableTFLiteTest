package com.example.appphone

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.RelativeLayout
import com.example.app_phone.R
import com.example.appphone.glclass.GLSurf
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    lateinit var detectorTest: DetectorTest
    lateinit var classifierTest: ImageClassifierTest
    // Our OpenGL Surfaceview
    private var glSurfaceView: GLSurfaceView? = null
    private var video_preview:CustomViewMediaCodec?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Fullscreen mode
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // Attach our surfaceview to our relative layout from our main layout.
        val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)


        video_preview=CustomViewMediaCodec(this)
        rootRelativeLayout.addView(video_preview,0, layoutParams)
        video_preview!!.setupCustomViewMediaCodec(seekBar_videoPos,video_sw)

        // We create our Surfaceview for our OpenGL here.
//        glSurfaceView = GLSurf(this)
//        rootRelativeLayout.addView(glSurfaceView,0, layoutParams)



        detectorTest=DetectorTest(this)
        classifierTest=ImageClassifierTest(this)


    }




}
