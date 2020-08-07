package dev.binclub.binscure.api.runtime

import java.io.File
import java.util.logging.Logger

/**
 * @author cookiedragon234 02/Aug/2020
 */
object NativeRuntime {
	private val logger = Logger.getLogger("binscure")
	
	init {
		val nativeLibraryName = "binscure_native_1"
		val nativeExtensions = arrayOf("dll", "so")
		val classLoader = javaClass.classLoader
		
		for (extension in nativeExtensions) {
			val name = "$nativeLibraryName.$extension"
			val res = classLoader.getResourceAsStream("/$name")
			if (res == null) {
				logger.warning("Couldn't load Binscure native library [$name] from classLoader [$classLoader]")
				continue
			}
			
			val file = File.createTempFile(nativeLibraryName, extension)
			file.deleteOnExit()
			
			res.copyTo(file.outputStream())
			
			logger.info("Appended Binscure native library [$name] to the path")
			
			System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + file.parentFile.absolutePath)
		}
		System.loadLibrary("binscure_native_1")
	}
	
	/**
	 * Load class
	 * @param a byte array
	 */
	external fun a(a: Any, b: Any): Any
}
