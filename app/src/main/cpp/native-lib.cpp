#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_beautyapp_NativeLib_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("BeautyApp Native Modular Engine Loaded");
}
