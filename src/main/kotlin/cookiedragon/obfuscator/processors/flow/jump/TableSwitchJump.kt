package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.insnListOf
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
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				
				if (!CObfuscator.randomWeight(5)) {
					val jumps = mutableSetOf<OpaqueJumps.JumpInfo>()
					for (insn in method.instructions) {
						if (insn is JumpInsnNode && insn.opcode != GOTO) {
							val falseJump = LabelNode(Label())
							modifier.append(insn, InsnList().apply { add(falseJump) })
							jumps.add(OpaqueJumps.JumpInfo(insn, LabelNode(Label()), insn.label, falseJump))
						}
					}
					
					if (true) {
						val start = LabelNode(Label())
						val end = LabelNode(Label())
						val switchStart = LabelNode(Label())
						val default = LabelNode(Label())
						
						val plus = random.nextInt(Integer.MAX_VALUE - (jumps.size * 2 + 2))
						
						val switchConditions = arrayListOf<LabelNode>()
						val beforeList = InsnList().apply {
							add(start)
							add(JumpInsnNode(GOTO, end))
							
							val randJump = JumpInsnNode(GOTO, start)
							var i = plus
							
							for (jump in jumps) {
								val switchEnter = LabelNode(Label())
								add(switchEnter)
								
								val trueSwitch = LabelNode(Label())
								val falseSwitch = LabelNode(Label())
								
								add(JumpInsnNode(jump.insn.opcode, trueSwitch))
								add(falseSwitch)
								add(ldcInt(i++))
								add(JumpInsnNode(GOTO, switchStart))
								add(trueSwitch)
								add(ldcInt(i++))
								add(JumpInsnNode(GOTO, switchStart))
								
								switchConditions.add(jump.falseJump)
								switchConditions.add(jump.trueJump)
								
								jump.insn.opcode = GOTO
								jump.insn.label = switchEnter
								
								if (random.nextBoolean()) {
									randJump.label = trueSwitch
								} else if (random.nextBoolean()) {
									randJump.label = falseSwitch
								}
							}
							switchConditions.add(start)
							switchConditions.add(default)
							
							add(default)
							add(randJump)
							
							add(end)
						}
						val afterList = InsnList().apply {
							add(switchStart)
							add(TableSwitchInsnNode(plus, (switchConditions.size - 1) + plus, default, *switchConditions.toTypedArray()))
						}
						method.instructions.insert(beforeList)
						method.instructions.add(afterList)
					}
				} else {
					for (insn in method.instructions) {
						if (insn is JumpInsnNode) {
							if (insn.opcode == GOTO)
								continue
							
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
								JumpInsnNode(insn.opcode, trueLabel),
								falseLabel,
								ldcInt(randInt - 1),
								JumpInsnNode(GOTO, switch),
								`else`
							)
							
							modifier.replace(insn, list)
						}
					}
				}
				modifier.apply(method)
			}
		}
	}
}
