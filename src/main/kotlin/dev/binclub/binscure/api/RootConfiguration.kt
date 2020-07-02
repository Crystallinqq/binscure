package dev.binclub.binscure.api

import dev.binclub.binscure.api.transformers.*
import dev.binclub.binscure.utils.replaceLast
import java.io.File

/**
 * @author cookiedragon234 25/Jan/2020
 */
data class RootConfiguration(
	val input: File,
	val output: File = File(input.absolutePath.replaceLast('.', "-obf.")),
	val mappingFile: File?,
	val libraries: List<File> = arrayListOf(),
	private val exclusions: List<String> = arrayListOf(),
	val hardExclusions: List<String> = arrayListOf(),
	val remap: RemapConfiguration,
	val sourceStrip: SourceStripConfiguration,
	val kotlinMetadata: KotlinMetadataConfiguration,
	val crasher: CrasherConfiguration,
	val indirection: IndirectionConfiguration,
	val stringObfuscation: StringObfuscationConfiguration,
	val flowObfuscation: FlowObfuscationConfiguration,
	val optimisation: OptimisationConfiguration,
	val numberObfuscation: NumberObfuscationConfiguration,
	val ignoreClassPathNotFound: Boolean = false,
	val useJavaClassloader: Boolean = false,
	val shuffleClasses: Boolean = true,
	val shuffleMethods: Boolean = true,
	val shuffleFields: Boolean = true,
	val resetLineProgress: Boolean = true,
	val printProgress: Boolean = true,
	val watermark: Boolean = true
): TransformerConfiguration(true, exclusions) {
	fun getLineChar(): Char = if (resetLineProgress) '\r' else '\n'
	
	override fun toString(): String  = """
		|RootConfig
		|   Input: $input
		|   Output: $output
		|   Libraries: $libraries
		|   MappingFile: $mappingFile
		|   Exclusions: $tExclusions
		|   Remap: $remap
		|   SourceStrip: $sourceStrip
		|   Kotlin Metadata: $kotlinMetadata
		|   Crasher: $crasher
		|   Indirection: $indirection
		|   StringObfuscation: $stringObfuscation
	""".trimMargin()
}
