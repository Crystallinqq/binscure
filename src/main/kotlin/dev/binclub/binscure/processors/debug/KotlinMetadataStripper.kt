package dev.binclub.binscure.processors.debug

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.transformers.KotlinMetadataType
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.InstructionModifier
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.objectweb.asm.Opcodes.POP
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import kotlin.jvm.internal.Intrinsics

/**
 * This transformer removes metadata emitted by the kotlin compiler
 *
 * @author cookiedragon234 22/Jan/2020
 */
object KotlinMetadataStripper: IClassProcessor {
	override val progressDescription: String
		get() = "Stripping kotlin metadata"
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.kotlinMetadata.enabled)
			return
		
		val remove = rootConfig.kotlinMetadata.type == KotlinMetadataType.REMOVE
		
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode)) continue
			
			classNode.visibleAnnotations = classNode.visibleAnnotations?.filter {it.desc != "Lkotlin/Metadata;" && it.desc != "Lkotlin/coroutines/jvm/internal/DebugMetadata;"}
			
			for (method in classNode.methods) {
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.owner == "kotlin/jvm/internal/Intrinsics") {
						if (insn.name == "checkParameterIsNotNull") {
							if (insn.previous is LdcInsnNode) {
								val prev = insn.previous as LdcInsnNode
								if (remove) {
									modifier.remove(prev)
									modifier.replace(insn, InsnNode(POP))
								} else {
									prev.cst = "."
								}
							}
						} else if (insn.name == "checkExpressionValueIsNotNull") {
							if (insn.previous is LdcInsnNode) {
								val prev = insn.previous as LdcInsnNode
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
			/*for (annotation in classNode.visibleAnnotations) {
				if (annotation.desc == "Lkotlin/Metadata;") {
					val header = createHeader(annotation)
					val metadata = KotlinClassMetadata.read(header)
					if (metadata is KotlinClassMetadata.Class) {
						val kClass = metadata.toKmClass()
					}
				}
			}*/
		}
	}
	
	/*private fun createHeader(node: AnnotationNode): KotlinClassHeader {
		var kind: Int? = null
		var metadataVersion: IntArray? = null
		var bytecodeVersion: IntArray? = null
		var data1: Array<String>? = null
		var data2: Array<String>? = null
		var extraString: String? = null
		var packageName: String? = null
		var extraInt: Int? = null
		
		val it = node.values.iterator()
		while (it.hasNext()) {
			val name = it.next() as String
			val value = it.next()
			
			when (name) {
				"k" -> kind = value as Int
				"mv" -> metadataVersion = listToIntArray(value)
				"bv" -> bytecodeVersion = listToIntArray(value)
				"d1" -> data1 = listToStringArray(value)
				"d2" -> data2 = listToStringArray(value)
				"xs" -> extraString = value as String
				"pn" -> packageName = value as String
				"xi" -> extraInt = value as Int
			}
		}
		
		return KotlinClassHeader(
			kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt
		)
	}*/
	@Suppress("UNCHECKED_CAST")
	private fun listToIntArray(list: Any): IntArray {
		return (list as List<Int>).stream().mapToInt { it }.toArray()
	}
	@Suppress("UNCHECKED_CAST")
	private fun listToStringArray(list: Any): Array<String> {
		return (list as List<String>).toTypedArray()
	}
}
