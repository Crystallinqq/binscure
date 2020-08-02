use std::finally::Finally;
use jni::JNIEnv;
use jni::objects::{JClass, JString, JByteArray, JObject};
use jni::sys::{jstring, jbyteArray};
use obfstr::obfstr;

#[no_mangle]
pub extern "system" fn Java_dev_binclub_binscure_api_runtime_NativeRuntime_a(
	env: JNIEnv,
	class: JClass,
	bytes_in: jbyteArray
	) {
	let (bytes, is_copy) = env.get_byte_array_elements(bytes_in).unwrap();
	(|| {
		let name = obfstr!("Hi");
		env.define_class()
	}).finally(|| {
		env.release_byte_array_elements(bytes_in, unsafe { bytes.as_mut().unwrap() }, ReleaseMode.NoCopyBack);
	})
}
