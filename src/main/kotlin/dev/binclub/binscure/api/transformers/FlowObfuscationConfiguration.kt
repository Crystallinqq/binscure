package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 07/Mar/2020
 */
data class FlowObfuscationConfiguration(
	override val enabled: Boolean = false,
	val severity: FlowObfuscationSeverity = FlowObfuscationSeverity.HARD
): TransformerConfiguration(enabled)

enum class FlowObfuscationSeverity {
	AGGRESSIVE(),
	SEVERE(),
	HARD(),
	NORMAL()
}
