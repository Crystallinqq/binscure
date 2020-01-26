package cookiedragon.obfuscator.api

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class IndirectionConfiguration(
	val enabled: Boolean = false,
	val type: IndirectionType = IndirectionType.INVOKEDYNAMIC
) {
	enum class IndirectionType {
		PROXY,
		INVOKEDYNAMIC
	}
}
