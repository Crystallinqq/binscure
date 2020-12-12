package dev.binclub.binscure.api.transformers

import dev.binclub.binscure.api.TransformerConfiguration

/**
 * @author cookiedragon234 26/Jan/2020
 */
data class CrasherConfiguration(
	override val enabled: Boolean = false,
	/// crash zip archivers with bad checksums
	val checksums: Boolean = true,
	/// crash decompilers with bad instructions
	val decompilers: Boolean = true,
	/// crash disassemblers with bad attributes
	val antiAsm: Boolean = true,
	/// crash recaf program with bad attributes
	val recaf: Boolean = false,
	private val exclusions: List<String> = arrayListOf()
): TransformerConfiguration(enabled, exclusions)
