package com.example.a86004771.wearabletest

import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import com.example.a86004771.glclass.GLSurf
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : WearableActivity()
{

    lateinit var detectorTest: DetectorTest
    lateinit var classifierTest: ImageClassifierTest
    // Our OpenGL Surfaceview
    private var glSurfaceView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Fullscreen mode
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // We create our Surfaceview for our OpenGL here.
        glSurfaceView = GLSurf(this)
        // Attach our surfaceview to our relative layout from our main layout.
        val glParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        rootRelativeLayout.addView(glSurfaceView,0, glParams)


        video_preview.setupCustomViewMediaCodec(seekBar_videoPos,video_sw)
//        detectorTest=DetectorTest(this)
//        classifierTest=ImageClassifierTest(this)


//        // Enables Always-on
        setAmbientEnabled()
    }



    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if(event?.repeatCount==0){
            Log.d("yama onKeyDown","keyCode="+event.keyCode)//265
        }


        return super.onKeyDown(keyCode, null)
    }
}



