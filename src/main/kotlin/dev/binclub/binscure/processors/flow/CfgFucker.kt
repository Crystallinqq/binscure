package dev.binclub.binscure.processors.flow

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.add
import dev.binclub.binscure.utils.hasAccess
import dev.binclub.binscure.processors.runtime.opaqueSwitchJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Feb/2020
 */
object CfgFucker: IClassProcessor {
	override val progressDescription: String
		get() = "Obfuscating method flow"
	override val config = rootConfig.flowObfuscation
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled) {
			return
		}
		val aggresiveness = config.severity
		
		for (classNode in classes.toTypedArray()) {
			if (isExcluded(classNode) || classNode.access.hasAccess(ACC_INTERFACE))
				continue
			
			for (method in classNode.methods) {
				if (method.instructions.size() < 5 || isExcluded(classNode, method))
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
