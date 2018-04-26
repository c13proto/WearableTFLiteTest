package com.example.common

import android.graphics.Bitmap
import android.util.Log
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

object CvUtils{

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


    fun outputByteBuffer(img:ByteBuffer){
        val byteArray = ByteArray(img.remaining())
        img.get(byteArray)
        outputByteArray(byteArray)
        img.clear()
    }
    fun outputByteArray(img: ByteArray) {

        try {
            val outputStream = FileOutputStream("/sdcard/Download/Raw")
            outputStream.write(img)
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.e("yama outputByteArray", e.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("yama outputByteArray", e.toString())
        }

    }

    fun convertYuvToBitmap(yuvBuffer: ByteArray,yuv_type: Int, width: Int, height: Int, pitch: Int,
                           dst_width: Int=width,dst_height: Int=height): Bitmap
    {
        val bitmap = Bitmap.createBitmap(dst_width, dst_height, Bitmap.Config.ARGB_8888)
        yuvToBitmap(yuvBuffer,yuv_type,width,height,pitch,bitmap,dst_width,dst_height)
        return bitmap
    }


    external fun yuvCropRotateToBgr(src_yuv: ByteArray, yuv_type: Int, src_width: Int, src_height: Int, src_pitch: Int, crop_area: IntArray, rotate: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    external fun yuvToBgr(src_yuv: ByteArray, yuv_type: Int, src_width: Int, src_height: Int, src_pitch: Int, dst: ByteBuffer, dst_width: Int, dst_height: Int, dst_ch: Int): Boolean
    external fun yuvToBitmap(src_yuv: ByteArray,yuv_type:Int,src_wisth:Int,src_height:Int,src_pitch:Int,bitmap: Bitmap,dst_width:Int=src_wisth,dst_height:Int=src_height):Boolean
    external fun getMotionArea(img1: ByteBuffer, img2: ByteBuffer, width: Int, height: Int, rect: IntArray): Boolean
    external fun bitmapToBgr(src_bitmpa:Bitmap,byteBuffer: ByteBuffer,dst_width: Int,dst_height: Int)
}
