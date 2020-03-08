package dev.binclub.binscure.processors.renaming.impl

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.kotlin.wrap
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @author cookiedragon234 22/Jan/2020
 */
object LocalVariableRenamer: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.remap.areLocalsEnabled())
			return
		
		val name = ConfigurationManager.rootConfig.remap.localVariableName
		for (classNode in CObfuscator.getProgressBar("Renaming Local Variables").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val nameMap = mutableMapOf<String, String>()
				
				if (name.isEmpty()) {
					method.localVariables = null
				} else {
					for (localVariable in method.localVariables ?: continue) {
						nameMap[localVariable.name] = "$name:${localVariable.desc}"
						localVariable.name = name
					}
				}
				
				if (method.parameters != null) {
					for (parameter in method.parameters) {
						parameter.name = name
					}
				}
				
				for (insn in method.instructions) {
					if (insn is LdcInsnNode && insn.cst is String) {
						val nextInsn = insn.next
						if (
							nextInsn != null
							&&
							nextInsn is MethodInsnNode
							&&
							nextInsn.owner == "kotlin/jvm/internal/Intrinsics"
							&&
							nextInsn.name == "checkParameterIsNotNull"
						) {
							val cst = insn.cst as String
							insn.cst = nameMap.getOrDefault(cst, "[Removed By CObf]")
						}
					}
				}
			}
		}
	}
}
