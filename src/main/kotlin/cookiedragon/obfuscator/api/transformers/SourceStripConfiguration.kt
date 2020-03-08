package cookiedragon.obfuscator.api.transformers

import cookiedragon.obfuscator.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class SourceStripConfiguration(
	override val enabled: Boolean = false,
	val lineNumbers: LineNumberAction = LineNumberAction.REMOVE
): TransformerConfiguration(enabled)

enum class LineNumberAction {
	KEEP,
	//SCRAMBLE,
	REMOVE
}
