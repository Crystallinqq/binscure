package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class SourceStripConfiguration(
	override val enabled: Boolean = false,
	val lineNumbers: LineNumberAction = LineNumberAction.REMOVE,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)

enum class LineNumberAction {
	KEEP,
	//SCRAMBLE,
	REMOVE
}
