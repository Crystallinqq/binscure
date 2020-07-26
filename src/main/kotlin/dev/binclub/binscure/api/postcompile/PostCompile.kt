package dev.binclub.binscure.api.postcompile

import dev.binclub.binscure.api.postcompile.BinscurePostCompile.Companion.invokeVirtual
import dev.binclub.binscure.api.postcompile.BinscurePostCompile.Companion.ldc
import dev.binclub.binscure.api.postcompile.BinscurePostCompile.Companion.pop

/**
 * @author cookiedragon234 13/Jul/2020
 */
interface BinscurePostCompile {
	fun ldc(str: String)
	fun ldc(clazz: Class<*>)
	fun pop()
	fun <T: Any?> invokeStatic(owner: Class<*>, name: String, desc: String): T
	fun <T: Any?> invokeStatic(owner: String, name: String, desc: String): T
	fun <T: Any?> invokeVirtual(instance: Any, owner: Class<*>, name: String, desc: String): T
	fun <T: Any?> invokeVirtual(instance: Any, owner: String, name: String, desc: String): T
	
	companion object: BinscurePostCompile {
		override fun ldc(str: String) {
			TODO("Not yet implemented")
		}
		
		override fun ldc(clazz: Class<*>) {
			TODO("Not yet implemented")
		}
		
		override fun pop() {
			TODO("Not yet implemented")
		}
		
		override fun <T: Any?> invokeStatic(owner: Class<*>, name: String, desc: String): T {
			TODO("Not yet implemented")
		}
		
		override fun <T: Any?> invokeStatic(owner: String, name: String, desc: String): T {
			TODO("Not yet implemented")
		}
		
		override fun <T: Any?> invokeVirtual(instance: Any, owner: Class<*>, name: String, desc: String): T {
			TODO("Not yet implemented")
		}
		
		override fun <T: Any?> invokeVirtual(instance: Any, owner: String, name: String, desc: String): T {
			TODO("Not yet implemented")
		}
	}
}

fun example() {
	ldc("hi")
	pop()
	val str = ""
	val bytes = invokeVirtual<ByteArray>(str, String::class.java, "getBytes", "()[B")
}
