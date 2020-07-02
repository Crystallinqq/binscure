package dev.binclub.binscure.api

import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.configuration.exclusions.PackageBlacklistExcluder
import dev.binclub.binscure.utils.asMutableList

/**
 * @author cookiedragon234 08/Mar/2020
 */
open class TransformerConfiguration(open val enabled: Boolean = false, exclusionsStr: List<String> = arrayListOf()) {
	val tExclusions= exclusionsStr.filter { it.isNotBlank() }.map {
		PackageBlacklistExcluder(it.trim())
	}
}
