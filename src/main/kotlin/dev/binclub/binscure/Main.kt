package dev.binclub.binscure

import dev.binclub.binscure.configuration.ConfigurationManager
import java.io.File
import java.io.FileNotFoundException

/**
 * @author cookiedragon234 20/Jan/2020
 */
object Main {
	fun main(args: Array<String>) {
		if (args.isEmpty()) {
			throw IllegalArgumentException("A config file must be provided")
		}
		val configFile = File(args[0])
		if (!configFile.exists()) {
			throw FileNotFoundException("Could not find file $configFile")
		}
		ConfigurationManager.init(configFile)
		CObfuscator()
	}
}
