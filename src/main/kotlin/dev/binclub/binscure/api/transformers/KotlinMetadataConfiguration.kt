package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class KotlinMetadataConfiguration(
	override val enabled: Boolean = false,
	val type: KotlinMetadataType = KotlinMetadataType.CENSOR
): TransformerConfiguration(enabled)

enum class KotlinMetadataType {
	/**
	 * With remove, all calls to Intrinsics will be removed
	 */
	REMOVE,
	/**
	 * With censor, any unnecessary parameters will be removed
	 */
	CENSOR
}
