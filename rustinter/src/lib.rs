#![allow(non_snake_case, non_camel_case_types)]

use std::ffi::c_void;
use std::ptr::null;
use jni::{JNIEnv, JavaVM, JNIVersion, NativeMethod};
use jni::objects::{JClass, ReleaseMode, JObject};
use jni::sys::{jbyteArray, jint, jclass, JNI_ERR};

use obfstr::obfstr;
use jni::strings::JNIString;

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
			sig: JNIString::from(obfstr!("(Ljava/lang/Object;)Ljava/lang/Object;")),
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
	bytes_in: jbyteArray
) -> jclass {
	let autoBytes = env.get_auto_byte_array_elements(bytes_in, ReleaseMode::NoCopyBack).unwrap();
	env.get_array_length(bytes_in).expect("");
	let classBytes: *const jbyte = autoBytes.as_ptr();
	env.define_class(null(), JObject::null(), autoBytes.as_ptr());
	jni_non_null_call!(
            self.internal,
            DefineClass,
            name.as_ptr(),
            loader.into_inner(),
            buf.as_ptr() as *const jbyte,
            buf.len() as jsize
        );
	return JObject::null().into_inner();
}
