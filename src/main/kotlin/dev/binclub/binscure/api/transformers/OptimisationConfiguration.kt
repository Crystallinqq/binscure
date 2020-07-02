package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 24/Mar/2020
 */
data class OptimisationConfiguration(
	override val enabled: Boolean = false,
	val mutableEnumValues: Boolean = false,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)
