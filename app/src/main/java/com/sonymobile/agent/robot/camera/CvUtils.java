package com.sonymobile.agent.robot.camera;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.ByteBuffer;

public final class CvUtils {

    static {
        try {
            System.loadLibrary("cvUtils");
            Log.d("yama cvUtils","LibraryFound");
        } catch (UnsatisfiedLinkError noLibraryFound) {
            Log.e("yama cvUtils","noLibraryFound");
            // Do nothing
        }
    }

    public static boolean nativeYuvCropRotateToRgb(byte[] src_yuv, boolean isNV21, int src_width, int src_height, int[] crop_area, int rotate, ByteBuffer dst, int dst_width, int dst_height, int dst_ch){
        return yuvCropRotateToRgb(src_yuv,isNV21,src_width,src_height,src_height,crop_area,rotate,dst,dst_width,dst_height,dst_ch);
    }
    public static boolean nativeYuvToRgb(byte[] src_yuv,boolean isNV21,int src_width,int src_height,ByteBuffer dst,int dst_width,int dst_height,int dst_ch){
        return yuvToRgb(src_yuv,isNV21,src_width,src_height,src_height,dst,dst_width,dst_height,dst_ch);
    }
    public static boolean nativeYuvToBitmap(byte[] src_yuv,boolean isNV21,int src_width,int src_height,int src_pitch,Bitmap bitmap){
        return yuvToBitmap(src_yuv,isNV21,src_width,src_height,src_pitch,bitmap);
    }


    private static native boolean yuvCropRotateToRgb(byte[] src_yuv,boolean isNV21,int src_width,int src_height,int src_pitch,int[] crop_area,int rotate,ByteBuffer dst,int dst_width,int dst_height,int dst_ch);
    private static native boolean yuvToRgb(byte[] src_yuv,boolean isNV21,int src_width,int src_height,int src_pitch,ByteBuffer dst,int dst_width,int dst_height,int dst_ch);
    private static native boolean yuvToBitmap(byte[] src_yuv,boolean isNV21,int src_width,int src_height,int src_pitch,Bitmap bitmap);
}
