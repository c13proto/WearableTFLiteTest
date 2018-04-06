package com.sonymobile.agent.robot.camera

import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer

class CvUtils{
    init {
        try {
            System.loadLibrary("cvUtils")
            Log.d("yama cvUtils", "LibraryInit")
        } catch (noLibraryFound: UnsatisfiedLinkError) {
            Log.e("yama cvUtils", "noLibraryNotFound")
        }
    }


    external fun yuvCropRotateToRgb(src_yuv: ByteArray, isNV21: Boolean, src_width: Int, src_height: Int, src_pitch: Int, crop_area: IntArray, rotate: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    external fun yuvToRgb(src_yuv: ByteArray, isNV21: Boolean, src_width: Int, src_height: Int, src_pitch: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    external fun yuvToBitmap(src_yuv: ByteArray,isNV21:Boolean,src_wisth:Int,src_height:Int,src_pitch:Int,bitmap: Bitmap):Boolean
}
