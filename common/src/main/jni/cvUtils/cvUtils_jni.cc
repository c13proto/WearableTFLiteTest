#include <jni.h>
#include <opencv2/core/hal/interface.h>
#include <android/log.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc/types_c.h>
#include <opencv/cv.hpp>
#include <android/bitmap.h>

using namespace cv;

#define JNI_METHOD(return_type, method_name) \
    extern "C" JNIEXPORT return_type JNICALL Java_com_example_common_CvUtils_##method_name

Mat YuvToBGR(jbyte* src,int width,int height, int pitch,int yuv_type);
Mat YuvToGray(jbyte* src,int width,int height);
Mat bytearrayToMat(const uchar* img,int width,int height,int ch);
bool ImageOutput(const uchar *img,int height,int width,int ch,char* filename);
enum YUV{I420=0,NV12=1,NV21=2};
JNI_METHOD(jboolean,yuvToBitmap)(JNIEnv* env, jobject, const jbyteArray src_yuv,jint yuv_type,jint src_width,jint src_height,jint src_pitch,jobject dst_bitmap,jint dst_width,jint dst_height){
    jboolean isCopy;
    jbyte *img_src = env->GetByteArrayElements(src_yuv, &isCopy);
    Mat yuv=Mat(src_pitch+src_pitch/2, src_width, CV_8UC1,img_src);
    Mat rgba = Mat(src_pitch, src_width, CV_8UC4);

    static int img_check=0;
//    __android_log_print(ANDROID_LOG_DEBUG, "cvUtils", "yuvToBitmap");
    switch(yuv_type){
        case YUV::I420 :cvtColor(yuv, rgba, CV_YUV2RGBA_I420);break;
        case YUV ::NV12 :cvtColor(yuv, rgba, CV_YUV2RGBA_NV12);break;
        case YUV ::NV21 :cvtColor(yuv, rgba, CV_YUV2RGBA_NV21);break;
        default:
            __android_log_print(ANDROID_LOG_ERROR, "yuvToBitmap","yuvTYPE incorrect!");
            break;
    }
    rgba=rgba(Rect(0,0,src_width,src_height));
    if(dst_width!=src_width||dst_height!=src_height){
        resize(rgba, rgba, Size(dst_width,dst_height));
    }



    unsigned char *pDst;
    if(AndroidBitmap_lockPixels(env, dst_bitmap, reinterpret_cast<void **>(&pDst))!=ANDROID_BITMAP_RESULT_SUCCESS)return false;
    memcpy(pDst,rgba.data,sizeof(jbyte)*rgba.total()*rgba.elemSize());
    AndroidBitmap_unlockPixels(env, dst_bitmap);

    if(img_check==0){
//        Mat rgb=Mat(src_pitch,src_width,CV_8UC3);
//        cvtColor(yuv,rgb,CV_YUV2RGB_NV12);
//        imwrite("/sdcard/img.jpg",rgb);
//        rgb.release();
    }
    yuv.release();
    rgba.release();

    if(img_check<100)img_check++;
    return true;
}
JNI_METHOD(jboolean,bitmapToBgr)(JNIEnv* env, jobject,jobject src_bitmap, jobject dst,jint dst_width, jint dst_height){
    unsigned char *p_dst = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(dst));
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, src_bitmap, &info);
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888){
        __android_log_print(ANDROID_LOG_ERROR, "yama bitmapToBgr:","FORMAT_ERROR") ;
        return false;
    }
    unsigned char *p_src;
    if(AndroidBitmap_lockPixels(env, src_bitmap, reinterpret_cast<void **>(&p_src))!=ANDROID_BITMAP_RESULT_SUCCESS)return false;
    static int img_check=0;

    Mat rgba=Mat(info.height,info.width,CV_8UC4,p_src);
    Mat bgr=Mat(info.height,info.width,CV_8UC3);
    cvtColor(rgba, bgr, CV_RGBA2BGR);
//    if(img_check==0) __android_log_print(ANDROID_LOG_DEBUG, "yama bitmapToBgr:","%d" ,imwrite("/sdcard/Download/rgba.jpg",bgr)) ;
    resize(bgr,bgr,Size(dst_width,dst_height));

    memcpy(p_dst,bgr.data,sizeof(jbyte)*bgr.total()*bgr.elemSize());
//    if(img_check==0) __android_log_print(ANDROID_LOG_DEBUG, "yama bitmapToBgr:","%d" ,imwrite("/sdcard/Download/bgr.jpg",bgr)) ;
//    if(img_check==0) __android_log_print(ANDROID_LOG_DEBUG, "yama bitmapToBgr:","%d" ,imwrite("/sdcard/Download/rgba.jpg",rgba)) ;
    AndroidBitmap_unlockPixels(env, src_bitmap);
    rgba.release();
    bgr.release();
    if(img_check<100)img_check++;
    return true;
}
JNI_METHOD(jboolean,yuvCropRotateToBgr)(JNIEnv* env, jobject, const jbyteArray src_yuv,jint yuv_type,jint src_width,jint src_height,jint src_pitch,const jintArray crop_area,jint rotate,
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
    if(dst_ch==3)converted=YuvToBGR(img_src,src_width,src_height,src_pitch,yuv_type);
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

JNI_METHOD(jboolean,yuvToBgr)(JNIEnv* env, jobject, const jbyteArray src_yuv,jint yuv_type,jint src_width,jint src_height,jint src_pitch,
                                jobject dst_bytebuffer,jint dst_width,jint dst_height,jint dst_ch){
    jboolean isCopy;
    jbyte *img_src = env->GetByteArrayElements(src_yuv, &isCopy);
    uchar *img_dst = reinterpret_cast<uchar *>(env->GetDirectBufferAddress(dst_bytebuffer));
    Mat converted;
    if(dst_ch==3)converted=YuvToBGR(img_src,src_width,src_height,src_pitch,yuv_type);
    else if(dst_ch==1)converted=YuvToGray(img_src,src_width,src_height);

    static int img_check=0;
    resize(converted,converted,Size(dst_width,dst_height));
    memcpy(img_dst,converted.data,sizeof(jbyte)*converted.total()*converted.elemSize());
//    if(img_check==0)imwrite("/sdcard/yuvToBgr.jpg",converted);
    converted.release();

    env->ReleaseByteArrayElements(src_yuv, img_src, JNI_ABORT);

    if(img_check<=10)img_check++;
    return true;
}
JNI_METHOD(jboolean,getMotionArea)(JNIEnv* env,jobject, const jobject nwImg,const jobject preImg, jint width, jint height,jintArray rect) {
    uchar* img1 = reinterpret_cast<uchar*>(env->GetDirectBufferAddress(nwImg));
    uchar* img2 = reinterpret_cast<uchar*>(env->GetDirectBufferAddress(preImg));
    Mat gray1=bytearrayToMat(img1,width,height,1);
    Mat gray2=bytearrayToMat(img2,width,height,1);
    Mat dst= Mat(height, width, CV_8UC1);
//    __android_log_print(ANDROID_LOG_DEBUG, "getMotionArea","1");
        static int img_check;
//        if(img_check==0){
//            ImageOutput(img1,height,width,1,"/sdcard/Download/img1.jpg");
//            ImageOutput(img2,height,width,1,"/sdcard/Download/img2.jpg");
//        }
        if(img_check<=10)img_check++;
//    auto t1 = std::chrono::system_clock::now();
    absdiff(gray1,gray2,dst);//差分
    gray1.release();gray2.release();
    double ave=mean(dst)[0];//輝度値が低かったら
//    __android_log_print(ANDROID_LOG_DEBUG, "yama", "ave=%f",ave);
    if(ave<0.5 || ave>10){//変化が乏しいか大きすぎるときは動いている領域が無いと判断
//            __android_log_print(ANDROID_LOG_ERROR, "getMotionArea","return ave %f",ave);
        return false;
    }
    threshold(dst,dst,0,255,THRESH_OTSU);//2値化(パラメータ値最適化できるかもしれないがとりあえず大津で)
    blur(dst,dst,Size(9,9));//平滑化
    threshold(dst,dst,0,255,THRESH_OTSU);//ごみを消す
//    auto t2 = std::chrono::system_clock::now();

    // 輪郭の検出
    std::vector<std::vector<Point> > contours;
    findContours(dst, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE);
    dst.release();
//    auto t3 = std::chrono::system_clock::now();
//    __android_log_print(ANDROID_LOG_ERROR, "yama contours.size:","%d",contours.size()) ;
    if(contours.size()<=0 || contours.size()>10) {
//            __android_log_print(ANDROID_LOG_ERROR, "getMotionArea","return size %d",contours.size());
        return false;//輪郭取れなかったときはリターン、多すぎるときも信頼性下がるのでリターン（速度的にも後の処理が重くなる）
    }
    // 最大面積の領域をピックアップ
    int maxContourIndex=0;
    double max_area=0;
    for(int i=0;i<contours.size();i++){
        double area=contourArea(contours[i]);
        if(area>max_area){
            max_area=area;
            maxContourIndex=i;
        }
    }
    Rect maxRect = boundingRect(contours[maxContourIndex]);

//    if(maxRect.height>height/2||maxRect.width>width/2){
////            __android_log_print(ANDROID_LOG_ERROR, "getMotionArea","src h,w=%d,%dresult height,width=%d,%d ",height,width,result.Height,result.Width);
//        return false;
//    }

    jint *rectPtr=env->GetIntArrayElements(rect,0);
    rectPtr[0]=maxRect.x;
    rectPtr[1]=maxRect.y;
    rectPtr[2]=maxRect.width;
    rectPtr[3]=maxRect.height;
    env->ReleaseIntArrayElements(rect, rectPtr, 0);

//    __android_log_print(ANDROID_LOG_ERROR, "getMotionArea","src h,w=%d,%dresult height,width=%d,%d ",height,width,maxRect.width,maxRect.height);

#ifdef IMAGE_OUT_DEBUG
    static int counter=0;
        if(counter==10) {
            ImageOutput(img1,height,width,1,"/sdcard/Download/img1.jpg");
            Mat debug = Mat(height, width, CV_8UC3, (0, 0, 0));
            for (size_t idx = 0; idx < contours.size(); idx++) {
                CvScalar color = CV_RGB(rand() & 255, rand() & 255, rand() & 255);
                drawContours(debug, contours, idx, color, -1);
            }
            rectangle(debug, maxRect, Scalar(0, 0, 255), 2);
            imwrite("/sdcard/Download/dst.jpg", dst);
            imwrite("/sdcard/Download/debug.jpg", debug);
            debug.release();
        }
        if(counter<100)counter++;
#endif


    return true;
}

Mat YuvToBGR(jbyte* src,int width,int height, int pitch,int yuv_type){
    Mat yuv=Mat(pitch+pitch/2, width, CV_8UC1,src);
    Mat converted = Mat(pitch, width, CV_8UC3);

    switch(yuv_type){
        case YUV::I420 :cvtColor(yuv, converted, CV_YUV2BGR_I420);break;
        case YUV ::NV12 :cvtColor(yuv, converted, CV_YUV2BGR_NV12);break;
        case YUV ::NV21 :cvtColor(yuv, converted, CV_YUV2BGR_NV21);break;
        default:
            __android_log_print(ANDROID_LOG_ERROR, "yuvToBitmap","yuvTYPE incorrect!");
            break;
    }
    converted=converted(Rect(0,0,width,height));
    yuv.release();
    return converted;
}
Mat YuvToGray(jbyte* src,int width,int height){
    return Mat(height, width, CV_8UC1,src);
}

Mat bytearrayToMat(const uchar* img,int width,int height,int ch){
    Mat mat;
    if (ch == 1) mat = Mat(height,width, CV_8UC1,const_cast<uchar*>(img));
    else if(ch == 3)mat = Mat(height, width, CV_8UC3,const_cast<uchar*>(img));
    else if(ch == 4)mat = Mat(height, width, CV_8UC4,const_cast<uchar*>(img));
    return mat;
}
bool ImageOutput(const uchar *img,int height,int width,int ch,char* filename) {

    Mat mat=bytearrayToMat(img,width,height,ch);
    bool ret=imwrite(filename,mat);
    if(!ret) __android_log_print(ANDROID_LOG_ERROR, "ImageOut fail", "%s",filename);
    else  __android_log_print(ANDROID_LOG_ERROR, "ImageOut success", "%s",filename);

    mat.release();

    return ret;
}