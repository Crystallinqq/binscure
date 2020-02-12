package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.isStatic
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.runtime.randomOpaqueJump
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
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
							add(randomOpaqueJump(al))
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
					}
				}
				modifier.apply(method)
			}
		}
	}
	
	data class JumpInfo(val thisLabel: LabelNode, val opcode: Int, val trueJump: LabelNode, val falseJump: LabelNode?)
}
