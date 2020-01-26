package cookiedragon.obfuscator.api

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class RemapConfiguration(
	val enabled: Boolean = false,
	val classPrefix: String = "",
	val methodPrefix: String = "",
	val fieldPrefix: String = "",
	val localVariableName: String = "c",
	val classes: Boolean = true,
	val methods: Boolean = true,
	val fields: Boolean = true,
	val localVariables: Boolean = true,
	val annotationExclude: List<String> = arrayListOf()
) {
	fun areClassesEnabled() = classes && enabled
	fun areMethodsEnabled() = methods && enabled
	fun areFieldsEnabled() = fields && enabled
	fun areLocalsEnabled() = localVariables && enabled
}
