package com.bingait.binscure.runtime

import java.io.File
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant


/**
 * @author cookiedragon234 03/Feb/2020
 */
object BinscureNativeRuntime {
	// String decryptor
	external fun a(a: String): String
	// Invoke Dynamic
	external fun b(a: MethodHandles.Lookup, b: String, c: MethodType, d: Int, e: String, f: String, g: String): Any
	
	
	
	init {
		if (System.getProperty("os.name").contains("win")) {
			loadLibraryFromJar("binscure.dll")
		} else {
			loadLibraryFromJar("binscure.so")
		}
	}
	
	private var temporaryDir: File? = null
	
	fun loadLibraryFromJar(path: String) {
		val temp = File(temporaryDir, path.substring(path.lastIndexOf('/') + 1))
		try {
			Files.copy(javaClass.getResourceAsStream(path), temp.toPath(), StandardCopyOption.REPLACE_EXISTING)
		} catch (e: Exception) {
			temp.delete()
			throw e
		}
		try {
			System.load(temp.absolutePath)
		} finally {
			if (isPosixCompliant())
				temp.delete()
			else
				temp.deleteOnExit()
		}
	}
	
	private fun isPosixCompliant(): Boolean {
		return try {
			FileSystems.getDefault()
				.supportedFileAttributeViews()
				.contains("posix")
		} catch (e: Exception) {
			false
		}
	}
	
	private fun createTempDirectory(prefix: String): File {
		val tempDir = System.getProperty("java.io.tmpdir")
		val generatedDir = File(tempDir, prefix + Instant.now().toEpochMilli())
		if (!generatedDir.mkdir()) throw IOException("BinScure IO error 0x05")
		return generatedDir
	}
}
