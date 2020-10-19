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
	
	fun obfuscate(args: Array<String>) {
		val configFile = File(args.firstOrNull() ?: throw IllegalArgumentException("A config file must be provided"))
		if (!configFile.exists()) {
			throw FileNotFoundException("Config File [$configFile] does not exist")
		}
		
		rootConfig = ConfigurationManager.parse(configFile)
		CObfuscator()
	}
	
	fun obfuscate(configFile: String) {
		val configFile = File(configFile)
		if (!configFile.exists()) {
			throw FileNotFoundException("Config File [$configFile] does not exist")
		}
		
		rootConfig = ConfigurationManager.parse(configFile)
		CObfuscator()
	}
	
	fun obfuscate(configFile: File) {
		rootConfig = ConfigurationManager.parse(configFile)
		CObfuscator()
	}
	
	fun obfuscate(configuration: RootConfiguration) {
		rootConfig = configuration
		CObfuscator()
	}
}
