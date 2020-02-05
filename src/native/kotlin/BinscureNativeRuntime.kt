package com.bingait.binscure.native

import kotlinx.cinterop.CPointer

@Suppress("UNUSED_PARAMETER")
@CName("Java_org_jonnyzzz_jni_java_NativeHost_callInt")
fun callInt(env: CPointer<JNIEnvVar>, clazz: jclass, it: jint): jint {
	initRuntimeIfNeeded()
	Platform.isMemoryLeakCheckerActive = false
	
	println("Native function is executed with: $it")
	return it + 1
}
