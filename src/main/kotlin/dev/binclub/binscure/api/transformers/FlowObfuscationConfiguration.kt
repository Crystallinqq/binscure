package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 07/Mar/2020
 */
data class FlowObfuscationConfiguration(
	override val enabled: Boolean = false,
	val severity: Int = 5,
	val mergeMethods: MergeMethods = MergeMethods.BLOAT_CLASSES,
	val arithmetic: Boolean = true,
	val noverify: Boolean = false,
	val java8: Boolean = false,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)

enum class MergeMethods {
	EXISTING_CLASSES,
	BLOAT_CLASSES,
	NONE
}
