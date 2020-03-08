package cookiedragon.obfuscator.api.transformers

import cookiedragon.obfuscator.api.TransformerConfiguration

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
