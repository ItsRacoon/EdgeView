#include <jni.h>
#include <vector>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeview_NativeLib_processFrame(JNIEnv *env, jclass clazz, jbyteArray input, jint width, jint height) {
    jbyte *input_bytes = env->GetByteArrayElements(input, nullptr);
    if (input_bytes == nullptr) {
        return nullptr;
    }

    jsize length = env->GetArrayLength(input);
    std::vector<jbyte> output_vector(length);
    // Placeholder: copy input to output
    for (int i = 0; i < length; ++i) {
        output_vector[i] = input_bytes[i];
    }

    jbyteArray output = env->NewByteArray(length);
    env->SetByteArrayRegion(output, 0, length, output_vector.data());
    env->ReleaseByteArrayElements(input, input_bytes, 0);

    return output;
}
