package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.isStatic
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
import cookiedragon.obfuscator.utils.randomStaticInvoke
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

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
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				val modifier = InstructionModifier()
				
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						if (insn.opcode == GOTO)
							continue
						
						val trueL = LabelNode(Label())
						val proxyTrue = LabelNode(Label())
						val switch = LabelNode(Label())
						val falseGoto = LabelNode(Label())
						
						val rand = if (random.nextBoolean()) {
							random.nextInt(Integer.MAX_VALUE)
						} else {
							-random.nextInt(Integer.MAX_VALUE)
						}
						
						val newList = InsnList().apply {
							if (!method.isStatic()) {
								add(VarInsnNode(ALOAD, 0))
								add(InsnNode(MONITORENTER))
							}
							add(JumpInsnNode(insn.opcode, trueL))
							add(ldcInt(rand - 1))
							add(JumpInsnNode(GOTO, switch))
							add(trueL)
							add(ldcInt(rand))
							add(JumpInsnNode(GOTO, switch))
							add(proxyTrue)
							
							val al = LabelNode(Label())
							add(randomStaticInvoke())
							add(JumpInsnNode(IFNONNULL, al))
							if (!method.isStatic()) {
								add(VarInsnNode(ALOAD, 0))
								add(InsnNode(MONITORENTER))
							}
							add(al)
							if (!method.isStatic()) {
								add(VarInsnNode(ALOAD, 0))
								add(InsnNode(MONITOREXIT))
							}
							add(ldcInt(rand - 2))
							
							add(switch)
							add(TableSwitchInsnNode(rand - 1, rand, insn.label,    falseGoto, proxyTrue))
							
							add(falseGoto)
							if (!method.isStatic()) {
								add(VarInsnNode(ALOAD, 0))
								add(InsnNode(MONITOREXIT))
							}
						}
						
						
						modifier.replace(insn, newList)
						
						
						/*
						val trueInt: Int
						val falseInt: Int
						if (random.nextBoolean()) {
							trueInt = randInt
							falseInt = randInt - 1
						} else {
							trueInt = randInt - 1
							falseInt = randInt
						}
						
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
							ldcInt(trueInt),
							JumpInsnNode(GOTO, switch),
							dummyLabel,
							ldcInt(randInt - 2),
							switch,
							TableSwitchInsnNode(randInt - 1, randInt, `else`, dummyLabel, target),
							end,
							JumpInsnNode(insn.opcode, trueLabel),
							falseLabel,
							ldcInt(falseInt),
							JumpInsnNode(GOTO, switch),
							`else`
						)
						
						modifier.replace(insn, list)*/
					}
				}
				modifier.apply(method)
			}
		}
	}
	
	data class JumpInfo(val thisLabel: LabelNode, val opcode: Int, val trueJump: LabelNode, val falseJump: LabelNode?)
}
