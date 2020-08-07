#![allow(non_snake_case, non_camel_case_types)]

use std::ffi::{c_void, CString};
use std::ptr::{null, null_mut};
use jni::{JNIEnv, JavaVM, JNIVersion, NativeMethod};
use jni::objects::{JClass, ReleaseMode, JObject, JString};
use jni::sys::{jbyteArray, jstring, jint, jclass, JNI_ERR, };

use obfstr::obfstr;
use jni::strings::{JNIString, JNIStr};
use std::os::raw::c_char;
use std::borrow::Borrow;

#[no_mangle]
pub extern fn JNI_OnLoad(
	vmIn: *mut JavaVM,
	_reserved: *const c_void
) -> jint {
	println!("{}", obfstr!("Binscure Native Library loaded"));
	
	let vm =
		if let Some(vm) = unsafe { vmIn.as_ref() } {
			vm
		} else {
			return JNI_ERR;
		};
	
	let env =
		if let Ok(env) = vm.get_env() {
			env
		} else {
			return JNI_ERR;
		};
	
	//let env = vm.get_env()
	//	.expect(obfstr!("Couldn't load JNI Env..."));
	
	let class =
		if let Ok(c) = env.find_class(obfstr!("dev/binclub/binscure/api/runtime/NativeRuntime")) {
			c
		} else {
			return JNI_ERR;
		};
	
	//let class = env.find_class(obfstr!("dev/binclub/binscure/api/runtime/NativeRuntime"))
	//	.expect(obfstr!("Couldn't find binscure native runtime"));
	
	let methods = [
		NativeMethod {
			name: JNIString::from(obfstr!("a")),
			sig: JNIString::from(obfstr!("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")),
			fn_ptr: defineBinscureClass as *mut c_void
		}
	];
	
	if env.register_native_methods(class, &methods).is_err() {
		println!("{}", obfstr!("Failed to register binscure native methods"));
		return JNI_ERR;
	}
	
	return JNIVersion::V6.into();
}

pub extern "system" fn defineBinscureClass(
	env: JNIEnv,
	class: JClass,
	name: jstring,
	bytes_in: jbyteArray
) -> jclass {
	let autoBytes = env.get_auto_byte_array_elements(bytes_in, ReleaseMode::NoCopyBack).unwrap();
	let bytesLength = env.get_array_length(bytes_in)
		.expect(obfstr!("Couldn't find bytearray length"));
	let str = env.get_string_utf_chars(JString::from(name))
		.expect(obfstr!("Couldn't fetch name chars"));
	
	let jniStr = unsafe { JNIStr::from_ptr(str) };
	env.define_class_autobytearray(jniStr.to_owned(), JObject::null(), autoBytes, bytesLength);
	
	//env.define_class_autobytearray(unsafe {
	//	JNIString { internal: null_mut() } }, JObject::null(), autoBytes, bytesLength);
	
	return JObject::null().into_inner();
}
