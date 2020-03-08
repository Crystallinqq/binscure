package dev.binclub.binscure.processors.flow.classinit

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.transformers.FlowObfuscationSeverity
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.BlameableLabelNode
import dev.binclub.binscure.utils.InstructionModifier
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 10/Feb/2020
 */
object ClassInitMonitor: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.flowObfuscation.enabled) {
			return
		}
		var aggresiveness = FlowObfuscationSeverity.values().size - (rootConfig.flowObfuscation.severity.ordinal + 1)
		
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				
				for (insn in method.instructions) {
					if (insn is TypeInsnNode && insn.opcode == NEW) {
						val fakeJump = BlameableLabelNode()
						
						val heavy = CObfuscator.randomWeight(aggresiveness);
						
						val after = InsnList().apply {
							if (heavy) {
								add(InsnNode(DUP))
								add(InsnNode(MONITORENTER))
							}
							add(randomOpaqueJump(fakeJump))
							add(InsnNode(DUP))
							add(InsnNode(MONITORENTER))
							add(fakeJump)
							if (heavy) {
								add(InsnNode(DUP))
								add(InsnNode(MONITOREXIT))
							}
						}
						modifier.append(insn, after)
					}
				}
				modifier.apply(method)
			}
		}
	}
}
