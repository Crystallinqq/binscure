package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.insnListOf
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.TableSwitchInsnNode

/**
 * @author cookiedragon234 05/Feb/2020
 */
object TableSwitchJump: IClassProcessor {
	//val eqJumps = arrayOf(IF_ACMPEQ, IF_ACMPNE, IF_ICMPEQ, IF_ICMPNE)
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Jumps to switches").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				/*val possibleTargets = arrayListOf<JumpInsnNode>()
				
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						if (insn.opcode == GOTO)
							possibleTargets.add(insn)
					}
				}*/
				
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						if (insn.opcode == GOTO)
							continue
						
						val randInt = CObfuscator.random.nextInt(Integer.MAX_VALUE)
						var other: Int
						do {
							other = CObfuscator.random.nextInt(Integer.MAX_VALUE)
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
							JumpInsnNode(insn.opcode, trueLabel),
							falseLabel,
							ldcInt(randInt - 1),
							JumpInsnNode(GOTO, switch),
							`else`
						)
						
						modifier.replace(insn, list)
					}
				}
				modifier.apply(method)
			}
		}
	}
}
