package dev.binclub.binscure.processors.flow.jump

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 06/Feb/2020
 */
object OpaqueJumps: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (!CObfuscator.isExcluded(classNode)) {
				for (method in classNode.methods) {
					if (CObfuscator.noMethodInsns(method))
						continue
					
					val modifier = InstructionModifier()
					for (insn in method.instructions) {
						if (insn is JumpInsnNode && insn.opcode == GOTO) {
							
							val randInt = random.nextInt(Integer.MAX_VALUE)
							var other: Int
							do {
								other = random.nextInt(Integer.MAX_VALUE)
							} while (other == randInt || other == randInt - 1)
							
							val target = insn.label
							val `else` = newLabel()
							
							val start = newLabel()
							val trueLabel = newLabel()
							val falseLabel = newLabel()
							val dummyLabel = newLabel()
							val switch = newLabel()
							val end = newLabel()
							
							val list = insnListOf(
								start,
								JumpInsnNode(GOTO, end),
								trueLabel,
								ldcInt(randInt),
								JumpInsnNode(GOTO, switch),
								dummyLabel,
								ldcInt(other),
								switch,
								TableSwitchInsnNode(randInt - 1, randInt, `else`, dummyLabel, target),
								end,
								randomStaticInvoke(),
								JumpInsnNode(IFNONNULL, trueLabel),
								falseLabel,
								ldcInt(randInt - 1),
								JumpInsnNode(GOTO, switch),
								`else`,
								InsnNode(ACONST_NULL),
								InsnNode(ATHROW)
							)
						}
					}
					modifier.apply(method)
				}
			}
		}
	}
}
