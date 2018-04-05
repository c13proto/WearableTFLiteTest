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

//    fun nativeYuvCropRotateToRgb(src_yuv: ByteArray, isNV21: Boolean, src_width: Int, src_height: Int, src_pitch: Int, crop_area: IntArray, rotate: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean {
//        return myYuvCropRotateToRgb(src_yuv, isNV21, src_width, src_height, src_pitch, crop_area, rotate, dst, dst_width, dst_height, dst_ch)
//    }
//    fun nativeYuvToRgb(src_yuv: ByteArray, isNV21: Boolean, src_width: Int, src_height: Int, src_pitch: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean {
//        return myYuvToRgb(src_yuv, isNV21, src_width, src_height, src_pitch, dst, dst_width, dst_height, dst_ch)
//    }

    public external fun nativeYuvCropRotateToRgb(src_yuv: ByteArray, isNV21: Boolean, src_width: Int, src_height: Int, src_pitch: Int, crop_area: IntArray, rotate: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    //    private native boolean myYuvToBitmap(byte[] src_yuv,boolean isNV21,int src_width,int src_height,Bitmap bitmap);
    public external fun nativeYuvToRgb(src_yuv: ByteArray, isNV21: Boolean, src_width: Int, src_height: Int, src_pitch: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    public external fun nativeYuvToBitmap(src_yuv: ByteArray,isNV21:Boolean,src_wisth:Int,src_height:Int,src_pitch:Int,bitmap: Bitmap):Boolean
}