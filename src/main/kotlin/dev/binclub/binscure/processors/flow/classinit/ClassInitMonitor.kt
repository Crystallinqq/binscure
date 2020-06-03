package dev.binclub.binscure.processors.flow.classinit

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.processors.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.newLabel
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 10/Feb/2020
 */
object ClassInitMonitor: IClassProcessor {
	override val progressDescription: String
		get() = "Obfuscating class instance creation"
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.flowObfuscation.enabled) {
			return
		}
		val aggresiveness = rootConfig.flowObfuscation.severity
		
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				
				for (insn in method.instructions) {
					if (insn is TypeInsnNode && insn.opcode == NEW) {
						// We need to be sure that the new object is immediately initialised
						// This is to prevent someone running binscure twice on the same jar
						// Therefore adding multiple monitorenters on the uninitalised object
						// The JVM spec says that if the MONITORENTER vs MONITOREXIT counter is > 1 then the JVM
						// Will throw and exception
						if (insn.next.opcode != INVOKEVIRTUAL || (insn.next.opcode == DUP && insn.next.next.opcode != INVOKEVIRTUAL)) {
							continue
						}
						
						val fakeJump = newLabel()
						
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
