package dev.binclub.binscure.processors.flow.jump

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.*


/**
 * @author cookiedragon234 14/Apr/2020
 */
object JumpRearranger: IClassProcessor {
	override val progressDescription: String
		get() = "Rearranging jumps"
	override val config = rootConfig.flowObfuscation
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled)
			return
		
		for (classNode in classes) {
			if (isExcluded(classNode))
				continue
			
			methodLoop@for (method in classNode.methods) {
				if (isExcluded(classNode, method))
					continue
				
				val stacks = StackHeightCalculator.calculateStackHeight(classNode, method)
				val targets = mutableMapOf<JumpInsnNode, LabelNode>()
				var varI = if (method.access.hasAccess(ACC_STATIC)) 0 else 1
				val stores = hashSetOf<Int>()
				
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						if (stacks[insn.label]!!.size == 0) {
							targets[insn] = insn.label
						}
					}
					if (insn is VarInsnNode) {
						if (insn.opcode == ASTORE) {
							if (!stores.contains(insn.`var`)) {
								stores.add(insn.`var`)
							} else {
								//continue@methodLoop
							}
						}
						if (insn.`var` >= varI) {
							varI = insn.`var` + 1
						}
					}
				}
				
				if (targets.size > 2) {
					val append = InsnList().apply {
						val dflt = newLabel()
						val switchStart = newLabel()
						add(dflt)
						add(ICONST_M1)
						add(switchStart)
						//add(VarInsnNode(ILOAD, varI))
						val tblSwitch = TableSwitchInsnNode(
							0,
							targets.size - 1,
							dflt
						)
						tblSwitch.labels = fixedSizeList(targets.size)
						add(tblSwitch)
						
						var i = 0
						for ((jump, target) in targets.entries.shuffled()) {
							val newTarget = newLabel()
							jump.label = newTarget
							add(newTarget)
							add(ldcInt(i))
							add(JumpInsnNode(GOTO, switchStart))
							tblSwitch.labels[i] = target
							i += 1
						}
					}
					
					method.instructions.add(append)
					
					println("Added to ${classNode.name}.${method.name}")
				}
			}
		}
	}
}
