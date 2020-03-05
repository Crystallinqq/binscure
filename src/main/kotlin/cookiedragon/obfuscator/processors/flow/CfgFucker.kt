package cookiedragon.obfuscator.processors.flow

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.add
import cookiedragon.obfuscator.runtime.opaqueSwitchJump
import cookiedragon.obfuscator.runtime.randomOpaqueJump
import cookiedragon.obfuscator.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException

/**
 * @author cookiedragon234 27/Feb/2020
 */
object CfgFucker: IClassProcessor {
	// 0 = most aggressive, 10 = hardly at all (each potential target will have a 1/n-1 change to be obfuscated)
	var aggresiveness = 1
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes.toTypedArray()) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || method.instructions.size() < 5)
					continue
				
				val modifier = InstructionModifier()
				val endings = hashSetOf<InsnList>()
				for (insn in method.instructions) {
					if (
						insn.next != null
						&&
						random.nextInt(aggresiveness) == 0
					) {
						if (insn is MethodInsnNode || insn is FieldInsnNode || insn is VarInsnNode) {
							val (list, ending) = opaqueSwitchJump()
							modifier.prepend(insn, list)
							endings.add(ending)
						} else if (insn is JumpInsnNode && insn.opcode != GOTO) {
							val falseNum = randomInt()
							val trueNum = falseNum + 1
							val key = randomInt()
							val list = InsnList().apply {
								val trueLdc = newLabel()
								val switch = newLabel()
								val dflt = newLabel()
								val after = newLabel()
								add(JumpInsnNode(insn.opcode, trueLdc))
								add(dflt)
								add(ldcInt(falseNum xor key))
								add(JumpInsnNode(GOTO, switch))
								add(trueLdc)
								add(ldcInt(trueNum xor key))
								add(switch)
								add(ldcInt(key))
								add(IXOR)
								add(constructTableSwitch(
									falseNum,
									dflt,
									after, insn.label
								))
								add(after)
							}
							modifier.replace(insn, list)
						}
					}
				}
				for (ending in endings) {
					method.instructions.add(ending)
				}
				endings.clear()
				modifier.apply(method)
			}
		}
	}
}
