#include "native_utils.h"
#include <android/log.h>

#define LOG_TAG "native-utils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

cv::Mat nv21ToMatRGBA(JNIEnv* env, jbyteArray nv21, int width, int height) {
    jbyte* nv21_bytes = env->GetByteArrayElements(nv21, nullptr);
    if (nv21_bytes == nullptr) {
        LOGE("Failed to get byte array elements for NV21 data.");
        return cv::Mat();
    }

    cv::Mat yuv(height + height / 2, width, CV_8UC1, nv21_bytes);
    cv::Mat rgba;

    cv::cvtColor(yuv, rgba, cv::COLOR_YUV2RGBA_NV21);

    env->ReleaseByteArrayElements(nv21, nv21_bytes, JNI_ABORT);

    return rgba;
}

jbyteArray matToJByteArray(JNIEnv* env, const cv::Mat& mat) {
    if (mat.empty() || mat.type() != CV_8UC4) {
        LOGE("Mat is empty or not in CV_8UC4 format.");
        return nullptr;
    }

    size_t total_bytes = mat.total() * mat.elemSize();
    jbyteArray result = env->NewByteArray(total_bytes);
    if (result == nullptr) {
        LOGE("Failed to create new jbyteArray.");
        return nullptr;
    }

    env->SetByteArrayRegion(result, 0, total_bytes, reinterpret_cast<const jbyte*>(mat.data));

    return result;
}
