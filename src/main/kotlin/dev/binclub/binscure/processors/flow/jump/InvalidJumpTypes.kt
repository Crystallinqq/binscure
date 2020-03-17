package dev.binclub.binscure.processors.flow.jump

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.kotlin.add
import dev.binclub.binscure.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.newLabel
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode

/**
 * @author cookiedragon234 16/Mar/2020
 */
object InvalidJumpTypes: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			for (method in classNode.methods) {
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (random.nextInt(5) == 0) {
						val target = newLabel()
						val loop = newLabel()
						val list = InsnList().apply {
							add(ICONST_0)
							add(JumpInsnNode(IFEQ, target))
							add(loop)
							add(LdcInsnNode(Handle(H_INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)))
							add(JumpInsnNode(IFNE, loop))
							add(target)
						}
						modifier.append(insn, list)
						
					}
				}
				modifier.apply(method)
			}
		}
	}
}
