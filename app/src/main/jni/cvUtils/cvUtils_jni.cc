#include <jni.h>
#include <opencv2/core/hal/interface.h>
#include <android/log.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc/types_c.h>
#include <opencv/cv.hpp>
#include <android/bitmap.h>

using namespace cv;

#define JNI_METHOD(return_type, method_name) \
    extern "C" JNIEXPORT return_type JNICALL Java_com_sonymobile_agent_robot_camera_CvUtils_##method_name

Mat YuvToBGR(jbyte* src,int width,int height, int pitch,bool isNV21);
Mat YuvToGray(jbyte* src,int width,int height);


JNI_METHOD(jboolean,nativeYuvToBitmap)(JNIEnv* env, jobject, const jbyteArray src_yuv,jboolean isNV21,jint src_width,jint src_height,jint src_pitch,jobject dst_bitmap){
    jboolean isCopy;
    jbyte *img_src = env->GetByteArrayElements(src_yuv, &isCopy);


    Mat converted=YuvToBGR(img_src,src_width,src_height,src_pitch,isNV21);
    AndroidBitmapInfo info;
    if(AndroidBitmap_getInfo(env, dst_bitmap, &info)!=ANDROID_BITMAP_RESULT_SUCCESS)return false;
    if(info.format!=ANDROID_BITMAP_FORMAT_RGBA_8888){
        __android_log_print(ANDROID_LOG_ERROR, "nativeYuvToBitmap", "BitmapType!=RGBA_8888");
        return false;
    }

    return true;
}
JNI_METHOD(jboolean,nativeYuvCropRotateToRgb)(JNIEnv* env, jobject, const jbyteArray src_yuv,jboolean isNV21,jint src_width,jint src_height,jint src_pitch,const jintArray crop_area,jint rotate,
             jobject dst_bytebuffer,jint dst_width,jint dst_height,jint dst_ch) {
    jboolean isCopy;
    jbyte *img_src = env->GetByteArrayElements(src_yuv, &isCopy);
    uchar *img_dst = reinterpret_cast<uchar *>(env->GetDirectBufferAddress(dst_bytebuffer));

    jint *cropArea= env->GetIntArrayElements(crop_area, &isCopy);
    const int crop_x        = cropArea[0];
    const int crop_y        = cropArea[1];
    const int crop_width    = cropArea[2];
    const int crop_height   = cropArea[3];

    if (crop_height + crop_y > src_height || crop_width + crop_x > src_width ||
        crop_x < 0 || crop_y < 0) {
    //            __android_log_print(ANDROID_LOG_ERROR, "libObjectTracker_myImageCrop", "crop area incorrect[x,y,w,h]=[%d,%d,%d,%d]",crop_x,crop_y,crop_width,crop_height);
        return false;
    }
    if (!(rotate == 0 || rotate == 90 || rotate == -90||rotate==180)) {
    //            __android_log_print(ANDROID_LOG_ERROR, "libObjectTracker_myImageCrop","rotate %d is not support", rotate);
        return false;
    }

    static int img_check;
    Mat converted;
    if(dst_ch==3)converted=YuvToBGR(img_src,src_width,src_height,src_pitch,isNV21);
    else if(dst_ch==1)converted=YuvToGray(img_src,src_width,src_height);
    //        if(img_check==0)imwrite("/sdcard/Download/converted0.jpg",converted);

    Mat cropped;
    if(rotate==0||rotate==180) {//先に画像全体をリサイズしてから
        float scale_x=(float)dst_width / crop_width;
        float scale_y=(float) dst_height / crop_height;
        resize(converted, converted, Size((int)(src_width * scale_x),(int)(src_height * scale_y)));
        cropped=converted(Rect((int)(crop_x*scale_x),(int)(crop_y*scale_y),dst_width,dst_height));

    }
    else {//90°回転時はcropとdstのwとhが入れ替わる
        float scale_x=(float)dst_height/ crop_width;
        float scale_y=(float) dst_width / crop_height;
        resize(converted,converted,Size((int)(src_width * scale_x),(int)(src_height * scale_y)));
        cropped=converted(Rect((int)(crop_x*scale_x),(int)(crop_y*scale_y),dst_height,dst_width));
    }
    //        if(img_check==0)imwrite("/sdcard/Download/converted0.jpg",converted);
    Mat rotated;
    if(rotate==0)rotated=cropped.clone();//Cloneしないと正しい画像にならないみたい
    if(rotate==90)cv::rotate(cropped,rotated,ROTATE_90_CLOCKWISE);
    else if(rotate==180)cv::rotate(cropped,rotated,ROTATE_180);
    else if(rotate==-90)cv::rotate(cropped,rotated,ROTATE_90_COUNTERCLOCKWISE);
    //        if(dst_ch==1&&img_check==0)imwrite("/sdcard/Download/rotated.jpg",rotated);//ここまでOk
    memcpy(img_dst,rotated.data,sizeof(jbyte)*rotated.total()*rotated.elemSize());

    converted.release();
    cropped.release();
    rotated.release();

    if(img_check<=10)img_check++;
    env->ReleaseByteArrayElements(src_yuv, img_src, JNI_ABORT);
    env->ReleaseIntArrayElements(crop_area, cropArea, JNI_ABORT);
    return true;
    }

JNI_METHOD(jboolean,nativeYuvToRgb)(JNIEnv* env, jobject, const jbyteArray src_yuv,jboolean isNV21,jint src_width,jint src_height,jint src_pitch,
                                jobject dst_bytebuffer,jint dst_width,jint dst_height,jint dst_ch){
    jboolean isCopy;
    jbyte *img_src = env->GetByteArrayElements(src_yuv, &isCopy);
    uchar *img_dst = reinterpret_cast<uchar *>(env->GetDirectBufferAddress(dst_bytebuffer));
    Mat converted;
    if(dst_ch==3)converted=YuvToBGR(img_src,src_width,src_height,src_pitch,isNV21);
    else if(dst_ch==1)converted=YuvToGray(img_src,src_width,src_height);

    static int img_check;
    resize(converted,converted,Size(dst_width,dst_height));
    memcpy(img_dst,converted.data,sizeof(jbyte)*converted.total()*converted.elemSize());
    converted.release();

    env->ReleaseByteArrayElements(src_yuv, img_src, JNI_ABORT);
    if(img_check<=10)img_check++;
    return true;
}

Mat YuvToBGR(jbyte* src,int width,int height, int pitch,bool isNV21){
    Mat yuv=Mat(pitch+pitch/2, width, CV_8UC1,src);
    Mat converted = Mat(pitch, width, CV_8UC3);
    if(isNV21)cvtColor(yuv, converted, CV_YUV2BGR_NV21);
    else cvtColor(yuv, converted, CV_YUV2BGR_NV12);
    converted=converted(Rect(0,0,width,height));
    yuv.release();
    return converted;
}
Mat YuvToGray(jbyte* src,int width,int height){
    return Mat(height, width, CV_8UC1,src);
}

