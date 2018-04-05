/*
 * Copyright (C) 2016 Sony Mobile Communications Inc.
 * All rights, including secret rights, reserved.
 */
package com.sonymobile.agent.robot.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class CameraUtils {
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    static {
        try {
            System.loadLibrary("NativeCameraUtils");
            Log.d("yama NativeCameraUtils","LibraryFound");
        } catch (UnsatisfiedLinkError noLibraryFound) {
            Log.e("yama NativeCameraUtils","noLibraryFound");
            // Do nothing
        }
    }

    private static final String TAG = "CameraUtils";

    private CameraUtils() {
        // Do not instantiate this class
    }

    private static native int nativeConvertNv21ToArgb(byte[] yuv, int[] argb, int width, int height);

    private static native int nativeConvertNv21To8BitGrayscale(byte[] yuv, int[] argb, int width, int height);

    private static native int nativeConvertArgbTo8BitGrayscale(IntBuffer argb, ByteBuffer yuv, int width, int height);

    private static native int nativeConvertArgbToYuv444(IntBuffer argb, ByteBuffer yuv, int width, int height);

    private static native int nativeCreateImageForFaceDetection(byte[] src, ByteBuffer dst, float scale, int orientation, int width, int height);
    private static native int nativeCreateImageForFaceIdentify(byte[] src, ByteBuffer dst, float scale, int orientation, int width, int height);
    private static native int nativeCreateImageForGeneralObjectDetector(byte[] src, ByteBuffer dst, float scale, int orientation, int width, int height);


    private static native int nativeConvertNv21ToBitmap(ByteBuffer srcY, ByteBuffer srcU, ByteBuffer srcV, Bitmap dst);
    private static native int nativeConvertBitmapToRGB888(Bitmap src, ByteBuffer dst, int orientation, int dst_width, int dst_height);

    private static native int nativeConvertBitmapToGrayY(Bitmap src, ByteBuffer dst, int orientation, int reductPixel, int leftX, int topY, int dst_width, int dst_height);

    private static native int nativeConvertBitmapToNv21(Bitmap src, ByteBuffer dst, int orientation, int reductPixel, int leftX, int topY, int dst_width, int dst_height);
    private static native int nativeConvertBitmapToGray(Bitmap src, ByteBuffer dst, int orientation, int dst_width, int dst_height);
    /**
     * Resizes the given bitmap image to new size
     *
     * @param image     original image
     * @param newWidth  target width of the image
     * @param newHeight target height of the image
     * @return resized image
     */
    public static Bitmap resizeBitmap(Bitmap image, int newWidth, int newHeight) {
        float scaleWidth = ((float) newWidth) / image.getWidth();
        float scaleHeight = ((float) newHeight) / image.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
    }

    /**
     * Rotates the given bitmap (anti-clockwise)
     *
     * @param image   original image
     * @param degrees rotation angle
     * @return rotated image
     */
    public static Bitmap rotateBitmap(Bitmap image, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
    }

    public static Bitmap cropAndCreateSquareBitmap(Bitmap source) {
        if (source.getHeight() <= source.getWidth()) {
            return Bitmap.createBitmap(source,
                    source.getWidth() / 2 - source.getHeight() / 2,
                    0,
                    source.getHeight(),
                    source.getHeight());
        } else {
            return Bitmap.createBitmap(source,
                    source.getHeight() / 2 - source.getWidth() / 2,
                    0,
                    source.getWidth(),
                    source.getWidth());
        }
    }

    public static IntBuffer bitmapToIntBuffer(Bitmap bitmap, ByteBuffer buffer, int[] argb) {
        bitmap.getPixels(argb, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        IntBuffer intBuffer = buffer.asIntBuffer();
        intBuffer.put(argb);
        return intBuffer;
    }

    /**
     * Converts bitmap to grayscale bytebuffer
     *
     * @param brightness buffer where saving grayscale data
     * @param argb       argb value of the bitmap
     * @param width      width of the image
     * @param height     height of the image
     * @return grayscale buffer
     */
    public static ByteBuffer toGrayScaleByteBuffer(ByteBuffer brightness, IntBuffer argb, int width, int height) {
        nativeConvertArgbTo8BitGrayscale(argb, brightness, width, height);
        return brightness;
    }

    /**
     * Converts bitmap to yuv444
     *
     * @param yuv    buffer where saving yuv444 data. Should be exactly 3times larger than bitmap
     * @param argb   argb value of the bitmap
     * @param width  width of the image
     * @param height height of the image
     * @return yuv buffer
     */
    public static ByteBuffer toYuvByteBuffer(ByteBuffer yuv, IntBuffer argb, int width, int height) {
        nativeConvertArgbToYuv444(argb, yuv, width, height);
        return yuv;
    }

    /**
     * Converts nv21 (YUV420 semi-planar) to argb data
     *
     * @param data   original nv21 data
     * @param argb   buffer to save converted data
     * @param width  width of the nv21 image
     * @param height height of the nv21 image
     * @return error code
     */
    public static int convertNv21ToArgb(byte[] data, int[] argb, int width, int height) {
        return nativeConvertNv21ToArgb(data, argb, width, height);
    }

    /**
     * Converts nv21 (YUV420 semi-planar) to grayscale data
     *
     * @param data   original nv21 data
     * @param argb   buffer to save converted data
     * @param width  width of the nv21 image
     * @param height height of the nv21 image
     * @return error code
     */
    public static int convertNv21To8BitGrayscale(byte[] data, int[] argb, int width, int height) {
        return nativeConvertNv21To8BitGrayscale(data, argb, width, height);
    }

    /**
     * Converts nv21Buffer to image suitable for face detection
     *
     * @param nv21Buffer  original image
     * @param buffer      buffer allocated for converted image
     * @param scale       rescale(shrink) factor of bitmap image. Must be greater or equal to 1.
     * @param orientation image orientation
     * @param width       image width
     * @param height      image height
     */
    public static int createImageForFaceDetection(byte[] nv21Buffer, ByteBuffer buffer, float scale, int orientation, int width, int height) {
        return nativeCreateImageForFaceDetection(nv21Buffer, buffer, scale, orientation, width, height);
    }

    /**
     * Converts bitmap to image suitable for face identify
     *
     * @param nv21Buffer original image
     * @param buffer     buffer allocated for converted image
     * @param scale      rescale(shrink) factor of bitmap image. Must be greater or equal to 1.
     */
    public static int createImageForFaceIdentify(byte[] nv21Buffer, ByteBuffer buffer, float scale, int orientation, int width, int height) {
        return nativeCreateImageForFaceIdentify(nv21Buffer, buffer, scale, orientation, width, height);
    }

    /**
     * Converts bitmap to image suitable for GeneralObjectDetector
     *
     * @param nv21Buffer original image
     * @param buffer buffer allocated for converted image
     * @param scale  rescale(shrink) factor of bitmap image. Must be greater or equal to 1.
     */
    public static int createImageForGeneralObjectDetector(byte[] nv21Buffer, ByteBuffer buffer, float scale, int orientation, int width, int height) {
        return nativeCreateImageForGeneralObjectDetector(nv21Buffer, buffer, scale, orientation, width, height);
    }

    /**
     * Converts yuv raw data to bitmap using neon
     * The buffer must be nv21 format
     *
     * @param srcY image y buffer
     * @param srcU image u buffer
     * @param srcV image v buffer
     * @param dst  target bitmap instance
     * @return 0 if success otherwise negative number.
     */
    public static int convertNv21ToBitmap(ByteBuffer srcY, ByteBuffer srcU, ByteBuffer srcV, Bitmap dst) {
        return nativeConvertNv21ToBitmap(srcY, srcU, srcV, dst);
    }
    public static int convertBitmapToRGB888(Bitmap src, ByteBuffer dst, int orientation, int dst_width, int dst_height) {
        return nativeConvertBitmapToRGB888(src, dst, orientation, dst_width, dst_height);
    }

    public static int convertBitmapToGrayY(Bitmap src, ByteBuffer dst, int orientation, int reductPixel, int leftX, int topY, int dst_width, int dst_height) {
        return nativeConvertBitmapToGrayY(src, dst, orientation, reductPixel, leftX, topY, dst_width, dst_height);
    }

    public static int convertBitmapToNv21(Bitmap src, ByteBuffer dst, int orientation, int reductPixel, int leftX, int topY, int dst_width, int dst_height) {
        return nativeConvertBitmapToNv21(src, dst, orientation, reductPixel, leftX, topY, dst_width, dst_height);
    }
    public static int convertBitmapToGray(Bitmap src, ByteBuffer dst, int orientation, int dst_width, int dst_height) {
        return nativeConvertBitmapToGray(src, dst, orientation, dst_width, dst_height);
    }

    /**
     * Converts given filepath to uri form
     *
     * @param file file to convert the path to uri
     * @return Converted uri
     */
    public static Uri toUri(File file) {
        return Uri.fromFile(file);
    }

//    /**
//     * Create a target file for saving an image or video
//     * Will create the private file associated with application
//     *
//     * @param type    file type either image or video
//     *                <ul>
//     *                <li>{@link #MEDIA_TYPE_IMAGE}
//     *                <li>{@link #MEDIA_TYPE_VIDEO}
//     *                </ul>
//     * @param context application's context
//     * @return target file to save the media. {@code null} if failed creating file
//     */
//    public static File getOutputMediaFile(int type, Context context) {
//
//        File cameraDir = new File(context.getFilesDir(), CameraConfig.ROBOT_CAMERA_DIRECTORY);
//
//        // Create the storage directory if it does not exist
//        if (!cameraDir.exists()) {
//            if (!cameraDir.mkdirs()) {
//                return null;
//            }
//        }
//
//        // Create a media file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
//        File mediaFile;
//        if (type == MEDIA_TYPE_IMAGE) {
//            mediaFile = new File(cameraDir.getPath() + File.separator
//                    + "DSC_" + timeStamp + ".JPG");
//        } else if (type == MEDIA_TYPE_VIDEO) {
//            mediaFile = new File(cameraDir.getPath() + File.separator
//                    + "VID_" + timeStamp + ".MP4");
//        } else {
//            return null;
//        }
//
//        return mediaFile;
//    }

    public static Size selectSize(Size[] sizes, @Nullable Size maxSize) {
        Size selectedSize = sizes[0];
        for (Size size : sizes) {
            if (size.getWidth() < selectedSize.getWidth()) {
                selectedSize = size;
            }
        }
        for (Size size : sizes) {
            if (maxSize == null) {
                // Choose largest size
                if (selectedSize.getWidth() <= size.getWidth()
                        && selectedSize.getHeight() <= size.getHeight()) {
                    selectedSize = size;
                }
            } else {
                if ((selectedSize.getWidth() <= size.getWidth() && size.getWidth() <= maxSize.getWidth())
                        && (selectedSize.getHeight() <= size.getHeight() && size.getHeight() <= maxSize.getHeight())) {
                    selectedSize = size;
                }
            }
        }
        return selectedSize;
    }
}
