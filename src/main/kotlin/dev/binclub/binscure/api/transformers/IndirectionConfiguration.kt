package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class IndirectionConfiguration(
	override val enabled: Boolean = false,
	val type: IndirectionType = IndirectionType.INVOKEDYNAMIC
): TransformerConfiguration(enabled)

enum class IndirectionType {
	PROXY,
	INVOKEDYNAMIC
}
