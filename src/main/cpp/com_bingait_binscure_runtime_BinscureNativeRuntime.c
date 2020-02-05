#include <jni.h>
#include <stdio.h>

JNIEXPORT jstring JNICALL Java_com_bingait_binscure_runtime_BinscureNativeRuntime_a
	(JNIEnv *env, jobject obj, jstring, encStr) {
	return encStr;
}

JNIEXPORT jobject JNICALL Java_com_bingait_binscure_runtime_BinscureNativeRuntime_b
	(JNIEnv *env, jobject obj, jobject methodHandle, jstring str, jobject methodType, jint insnOp, jstring clazz, jstring method, jstring desc) {
	return obj;
}
