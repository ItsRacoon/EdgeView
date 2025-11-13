#ifndef EDGEVIEW_NATIVE_UTILS_H
#define EDGEVIEW_NATIVE_UTILS_H

#include <jni.h>
#include <opencv2/opencv.hpp>

cv::Mat nv21ToMatRGBA(JNIEnv* env, jbyteArray nv21, int width, int height);
jbyteArray matToJByteArray(JNIEnv* env, const cv::Mat& mat);

#endif //EDGEVIEW_NATIVE_UTILS_H
