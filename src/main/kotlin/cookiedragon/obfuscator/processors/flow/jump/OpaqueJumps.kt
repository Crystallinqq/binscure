package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.insnListOf
import cookiedragon.obfuscator.utils.ldcInt
import cookiedragon.obfuscator.utils.randomStaticInvoke
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 06/Feb/2020
 */
object OpaqueJumps: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Adding opauque jumps").wrap(classes)) {
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
							val `else` = LabelNode(Label())
							
							val start = LabelNode(Label())
							val trueLabel = LabelNode(Label())
							val falseLabel = LabelNode(Label())
							val dummyLabel = LabelNode(Label())
							val switch = LabelNode(Label())
							val end = LabelNode(Label())
							
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
