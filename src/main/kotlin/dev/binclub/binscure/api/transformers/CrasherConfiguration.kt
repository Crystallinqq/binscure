package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class CrasherConfiguration(
	override val enabled: Boolean = false,
	val checksums: Boolean = true,
	val antiAsm: Boolean = true,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)
