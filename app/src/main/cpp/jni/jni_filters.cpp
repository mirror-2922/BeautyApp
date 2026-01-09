#include <jni.h>
#include "../filters/filters.h"
#include "../utils/utils.h"

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_applyBeautyFilter(JNIEnv*, jobject, jlong matAddr) {
    applyBeauty(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_applyDehaze(JNIEnv*, jobject, jlong matAddr) {
    applyDehaze(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_applyUnderwater(JNIEnv*, jobject, jlong matAddr) {
    applyUnderwater(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_applyStage(JNIEnv*, jobject, jlong matAddr) {
    applyStage(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_convertToGray(JNIEnv*, jobject, jlong matAddr) {
    applyGray(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_histogramEqualization(JNIEnv*, jobject, jlong matAddr) {
    applyHistEq(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_binarize(JNIEnv*, jobject, jlong matAddr) {
    applyBinary(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_morphOpen(JNIEnv*, jobject, jlong matAddr) {
    applyMorphOpen(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_morphClose(JNIEnv*, jobject, jlong matAddr) {
    applyMorphClose(getMat(matAddr));
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_beautyapp_NativeLib_applyBlur(JNIEnv*, jobject, jlong matAddr) {
    applyBlur(getMat(matAddr));
}
