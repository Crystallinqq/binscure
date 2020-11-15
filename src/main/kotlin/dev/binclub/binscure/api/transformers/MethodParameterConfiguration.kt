package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * Applies obfuscation to method parameters
 *
 * @author cook 15/Nov/2020
 */
data class MethodParameterConfiguration(
	override val enabled: Boolean = false,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)
