package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class KotlinMetadataConfiguration(
	override val enabled: Boolean = false
): TransformerConfiguration(enabled)
