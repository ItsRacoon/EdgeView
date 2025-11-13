#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <chrono>
#include <vector>

#define LOG_TAG "native-lib"

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeview_NativeLib_processFrame(JNIEnv* env, jclass clazz, jbyteArray input, jint width, jint height) {
    auto start_time = std::chrono::high_resolution_clock::now();

    if (input == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Input jbyteArray is null.");
        return nullptr;
    }

    jsize input_len = env->GetArrayLength(input);
    if (input_len != width * height * 4) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Input byte array size mismatch. Expected %d, got %d", width * height * 4, input_len);
        return input;
    }

    jbyte* input_bytes = env->GetByteArrayElements(input, JNI_FALSE);
    if (input_bytes == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to get byte array elements.");
        return input;
    }

    cv::Mat rgba_mat(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(input_bytes));

    cv::Mat gray_mat;
    cv::cvtColor(rgba_mat, gray_mat, cv::COLOR_RGBA2GRAY);

    cv::Mat edges_mat;
    cv::Canny(gray_mat, edges_mat, 50, 150);

    cv::Mat processed_rgba_mat;
    cv::cvtColor(edges_mat, processed_rgba_mat, cv::COLOR_GRAY2RGBA);

    env->ReleaseByteArrayElements(input, input_bytes, JNI_ABORT);

    jbyteArray output_array = env->NewByteArray(width * height * 4);
    if (output_array == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to create new output byte array.");
        return nullptr;
    }

    unsigned char* output_bytes = processed_rgba_mat.data;
    env->SetByteArrayRegion(output_array, 0, width * height * 4, reinterpret_cast<jbyte*>(output_bytes));

    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "processFrame execution time: %lld ms", duration.count());

    return output_array;
}
