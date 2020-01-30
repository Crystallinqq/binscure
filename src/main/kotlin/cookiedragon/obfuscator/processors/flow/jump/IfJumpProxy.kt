package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
import cookiedragon.obfuscator.utils.randomThrowable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.Opcodes.IFNONNULL
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 29/Jan/2020
 */
object IfJumpProxy: IClassProcessor {
	val heavy = false
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Proxying if jumps").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						val tryStart = LabelNode(Label())
						val tryEnd = LabelNode(Label())
						val catch = LabelNode(Label())
						val after = LabelNode(Label())
						
						val val1 = LabelNode(Label())
						val switch = LabelNode(Label())
						
						val list = InsnList().apply {
							add(tryStart)
							add(JumpInsnNode(insn.opcode, val1))
							add(ldcInt(0))
							add(JumpInsnNode(GOTO, switch))
							add(catch)
							add(JumpInsnNode(IFNONNULL, val1))
							add(val1)
							add(ldcInt(-1))
							add(switch)
							add(TableSwitchInsnNode(-1, 0, val1, insn.label, after))
							add(tryEnd)
						}
						
						insn.label = tryStart
						insn.opcode = GOTO
						
						modifier.append(insn, InsnList().apply{add(after)})
						modifier.append(method.instructions.last, list)
						method.tryCatchBlocks.add(TryCatchBlockNode(tryStart, tryEnd, catch, randomThrowable()))
						
						println("Added to ${method.name}")
						break
					}
				}
				modifier.apply(method)
			}
		}
	}
}
