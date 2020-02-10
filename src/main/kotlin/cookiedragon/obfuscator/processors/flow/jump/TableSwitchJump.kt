package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.GOTO
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
				
				val realIndex = random.nextInt(3)
				var proxyTrueIndex: Int
				do {
					proxyTrueIndex = random.nextInt(3)
				} while (proxyTrueIndex != realIndex)
				var proxyFalseIndex: Int
				do {
					proxyFalseIndex = random.nextInt(3)
				} while (proxyFalseIndex != realIndex && proxyFalseIndex != proxyTrueIndex)
				
				
				
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						if (insn.opcode == GOTO)
							continue
						
						val switchStart = LabelNode(Label())
						val entryLabel = LabelNode(Label())
						val endLabel = LabelNode(Label())
						val proxyFalse = LabelNode(Label())
						val proxyTrue = LabelNode(Label())
						
						var randInt = 2//random.nextInt(Integer.MAX_VALUE)
						
						val jumps = arrayListOf<JumpInfo>(
							JumpInfo(entryLabel, insn.opcode, insn.label, endLabel)
							//JumpInfo(proxyTrue, GOTO, insn.label, null),
							//JumpInfo(proxyFalse, GOTO, endLabel, null)
						)//.shuffled(random)
						
						val switchLabels = arrayListOf<LabelNode>()
						
						val newList = InsnList().apply {
							add(JumpInsnNode(GOTO, entryLabel))
							
							for (jump in jumps) {
								val trueL = LabelNode(Label())
								add(jump.thisLabel)
								if (jump.falseJump != null && insn.opcode != GOTO) {
									add(JumpInsnNode(jump.opcode, trueL))
									add(ldcInt(randInt))
									randInt -= 1
									add(JumpInsnNode(GOTO, switchStart))
									
									switchLabels.add(jump.falseJump)
									
									add(trueL)
								}
								add(ldcInt(randInt))
								randInt -= 1
								add(JumpInsnNode(GOTO, switchStart))
								
								switchLabels.add(jump.trueJump)
							}
							
							add(switchStart)
							add(TableSwitchInsnNode(randInt, randInt + switchLabels.size, endLabel, *switchLabels.toTypedArray().reversedArray()))
							
							add(endLabel)
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
