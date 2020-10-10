package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cook 09/Oct/2020
 */
data class ArithmeticObfuscationConfiguration(
	override val enabled: Boolean = false,
	val repeat: Int = 1,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)
