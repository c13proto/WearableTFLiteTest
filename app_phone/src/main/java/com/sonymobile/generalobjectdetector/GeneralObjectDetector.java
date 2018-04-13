package com.sonymobile.generalobjectdetector;

import android.util.Log;


import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GeneralObjectDetector {
    public static final String TAG = "GeneralObjectDetector";

    public static final String SOD_NAME = "SOD_SR_BN0008-0000_D0800_C013_P288x288";

    static {
        System.loadLibrary("general_object_detector_jni");
    }

    private final long mNativeAddress;

    public GeneralObjectDetector() {
        mNativeAddress = createNative();
    }

    public void initialize(File sod, File initData) {
        if (isInitialized(mNativeAddress)) {
            Log.d(TAG, "GeneralObjectDetector has been initialized.");
            return;
        }

        initialize(mNativeAddress, sod.getAbsolutePath(), initData.getAbsolutePath());
    }

    public List<GeneralDetectedObject> detect(ByteBuffer rgbDirectBuffer, int imageWidth, int imageHeight) {
        if (!isInitialized(mNativeAddress)) {
            Log.d(TAG, "GeneralObjectDetector is not be initialized.");

            return Collections.emptyList();
        }

        return Arrays.asList(detect(mNativeAddress, rgbDirectBuffer, imageWidth, imageHeight));
    }

    public void release() {
        releaseNative(mNativeAddress);
    }

    private native long createNative();

    private native void initialize(long nativeAddress, String sodPath, String initDataPath);

    private native boolean isInitialized(long nativeAddress);

    private native GeneralDetectedObject[] detect(
            long nativeAddress, ByteBuffer rgbDirectBuffer, int imageWidth, int imageHeight);

    private native void releaseNative(long nativeAddress);
}
