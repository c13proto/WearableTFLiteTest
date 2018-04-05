package com.example.a86004771.wearabletest

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.wear.widget.BoxInsetLayout
import android.support.wearable.activity.WearableActivity
import android.transition.Visibility
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.MediaController
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.support.wearable.input.WearableButtons
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : WearableActivity()
{


    lateinit var  videoPath:String//="android.resource://" + packageName + "/raw/" + R.raw.dog
    lateinit var  videoURI:Uri// = Uri.parse(videoPath)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        videoPath="android.resource://" + packageName + "/raw/" + R.raw.dog
//        videoURI= Uri.parse(videoPath)

//        imageView.setImageResource(R.drawable.bluebells_clipped)
//        imageView.setImageURI(Uri.parse("android.resource://" + packageName + "/raw/" + R.drawable.bluebells_clipped))//これはOk
//        button.setOnClickListener {
//            if(imageView.visibility== View.VISIBLE)imageView.visibility=View.INVISIBLE
//            else imageView.visibility=View.VISIBLE
//        }


//        videoView動かない？エミュレータで表示されない
//        videoView.setMediaController(MediaController(this))
//        videoView.setVideoURI(videoURI)
        video_preview.setupCustomViewMediaCodec(seekBar_videoPos,video_sw)



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



