package com.sonymobile.agent.robot.camera

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import java.nio.ByteBuffer

class CvUtils{

    companion object {
        init {
            try {
                System.loadLibrary("cvUtils")
                Log.d("yama cvUtils", "LibraryInit")
            } catch (noLibraryFound: UnsatisfiedLinkError) {
                Log.e("yama cvUtils", "noLibraryNotFound")
            }
        }
        const val YUV_I420=0
        const val YUV_NV12=1
        const val YUV_NV21=2
    }


    external fun yuvCropRotateToRgb(src_yuv: ByteArray, yuv_type: Int, src_width: Int, src_height: Int, src_pitch: Int, crop_area: IntArray, rotate: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    external fun yuvToRgb(src_yuv: ByteArray, yuv_type: Int, src_width: Int, src_height: Int, src_pitch: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    external fun yuvToBitmap(src_yuv: ByteArray,yuv_type:Int,src_wisth:Int,src_height:Int,src_pitch:Int,bitmap: Bitmap,dst_width:Int=src_wisth,dst_height:Int=src_height):Boolean
    external fun getMotionArea(img1: ByteBuffer, img2: ByteBuffer, width: Int, height: Int, rect: IntArray): Boolean
}
