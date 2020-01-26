package cookiedragon.obfuscator.api

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class SourceStripConfiguration(
	val enabled: Boolean = false,
	val lineNumbers: LineNumberAction = LineNumberAction.REMOVE
) {
	enum class LineNumberAction {
		KEEP,
		//SCRAMBLE,
		REMOVE
	}
}
