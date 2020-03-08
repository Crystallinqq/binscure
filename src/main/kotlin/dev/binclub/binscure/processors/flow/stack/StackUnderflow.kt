package dev.binclub.binscure.processors.flow.stack

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.utils.BlameableLabelNode
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.randomStaticInvoke
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.IFNONNULL
import org.objectweb.asm.Opcodes.POP2
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 10/Feb/2020
 */
object StackUnderflow: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (!CObfuscator.isExcluded(classNode)) {
				for (method in classNode.methods) {
					val modifier = InstructionModifier()
					for (insn in method.instructions) {
						if (insn is JumpInsnNode) {
							val start = BlameableLabelNode()
							val before = InsnList().apply {
								add(randomStaticInvoke())
								add(JumpInsnNode(IFNONNULL, start))
								
								add(InsnNode(POP2))
								add(InsnNode(POP2))
								add(InsnNode(POP2))
								add(InsnNode(POP2))
								add(InsnNode(POP2))
								
								add(start)
							}
							
							modifier.prepend(insn, before)
						}
					}
					modifier.apply(method)
				}
			}
		}
	}
}
