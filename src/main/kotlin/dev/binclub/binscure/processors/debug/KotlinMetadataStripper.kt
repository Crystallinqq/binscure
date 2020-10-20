package dev.binclub.binscure.processors.debug

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.api.transformers.KotlinMetadataType
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.insnBuilder
import org.objectweb.asm.Opcodes.POP
import org.objectweb.asm.tree.*

/**
 * This transformer removes metadata emitted by the kotlin compiler
 *
 * @author cookiedragon234 22/Jan/2020
 */
object KotlinMetadataStripper: IClassProcessor {
	override val progressDescription: String
		get() = "Stripping kotlin metadata"
	override val config: TransformerConfiguration
		get() = rootConfig.kotlinMetadata
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled)
			return
		
		val remove = rootConfig.kotlinMetadata.type == KotlinMetadataType.REMOVE
		
		
		forClass(classes) { classNode ->
			classNode.visibleAnnotations?.listIterator()?.also { it ->
				it.forEach { annotation ->
					if (annotation.desc == "Lkotlin/Metadata;" || annotation.desc == "Lkotlin/coroutines/jvm/internal/DebugMetadata;") {
						it.remove()
					}
				}
			}
			
			forMethod(classNode) { method ->
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.owner == "kotlin/jvm/internal/Intrinsics") {
						if (insn.name == "checkParameterIsNotNull") {
							if (insn.desc == "(Ljava/lang/Object;Ljava/lang/String;)V") {
								val prev = insn.previous
								if (prev is LdcInsnNode) {
									if (remove) {
										modifier.remove(prev)
										modifier.replace(insn, InsnNode(POP))
									} else {
										prev.cst = "."
									}
								} else if (remove) {
									modifier.replace(insn, insnBuilder {
										pop()
										pop()
									})
								}
							} else if (remove) {
								modifier.replace(insn, insnBuilder {
									pop()
								})
							}
						} else if (insn.name == "checkExpressionValueIsNotNull") {
							val prev = insn.previous
							if (prev is LdcInsnNode) {
								if (remove) {
									modifier.remove(prev)
									modifier.replace(insn, InsnNode(POP))
								} else {
									prev.cst = "."
								}
							}
						}
					}
				}
				modifier.apply(method)
			}
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	private fun listToIntArray(list: Any): IntArray {
		return (list as List<Int>).stream().mapToInt { it }.toArray()
	}
	@Suppress("UNCHECKED_CAST")
	private fun listToStringArray(list: Any): Array<String> {
		return (list as List<String>).toTypedArray()
	}
}
