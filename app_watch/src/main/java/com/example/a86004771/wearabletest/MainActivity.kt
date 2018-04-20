package com.example.a86004771.wearabletest

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : WearableActivity()
{

    lateinit var classifierTest: ImageClassifierTest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        video_preview.setupCustomViewMediaCodec(seekBar_videoPos,video_sw,gl_preview,true)
//        detectorTest=DetectorTest(this)
        classifierTest=ImageClassifierTest(this)


//        // Enables Always-on
        setAmbientEnabled()
    }



    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if(event?.repeatCount==0){
            Log.d("yama onKeyDown","keyCode="+event.keyCode)//265
            Log.d("yama","finish()")
            this.finish()
        }


        return super.onKeyDown(keyCode, null)
    }
}



