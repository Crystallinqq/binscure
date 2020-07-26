package dev.binclub.binscure.api

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import java.io.File
import java.io.FileNotFoundException

/**
 * @author cookiedragon234 13/Jul/2020
 */
object Binscure {
	@JvmStatic
	fun main(args: Array<String>) = obfuscate(args)
	
	fun obfuscate(args: Array<String>) = obfuscate(args.also {
		if (args.isEmpty()) {
			throw IllegalArgumentException("A config file must be provided")
		}
	}.first())
	fun obfuscate(configFile: String) = obfuscate(File(configFile).also {
		if (!it.exists()) {
			throw FileNotFoundException("File [$it] does not exist")
		}
	})
	fun obfuscate(configFile: File) = obfuscate(ConfigurationManager.parse(configFile))
	fun obfuscate(configuration: RootConfiguration) {
		rootConfig = configuration
		
		CObfuscator()
	}
}
