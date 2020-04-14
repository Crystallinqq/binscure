package dev.binclub.binscure.processors.flow.jump

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.*


/**
 * @author cookiedragon234 14/Apr/2020
 */
object JumpRearranger: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val stacks = StackHeightCalculator.calculateStackHeight(classNode, method)
				val targets = mutableMapOf<JumpInsnNode, LabelNode>()
				var varI = if (method.access.hasAccess(ACC_STATIC)) 0 else 1
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						if (stacks[insn.label]!!.size == 0) {
							targets[insn] = insn.label
						}
					}
					if (insn is VarInsnNode) {
						if (insn.`var` >= varI) {
							varI = insn.`var` + 1
						}
					}
				}
				
				if (stacks.size > 5) {
					println("Storing in $varI")
					val prepend = InsnList().apply {
					}
					
					val append = InsnList().apply {
						val dflt = newLabel()
						val switchStart = newLabel()
						add(dflt)
						add(ldcInt(0))
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
					
					method.instructions.insert(prepend)
					method.instructions.add(append)
					
					println("Added to ${classNode.name}.${method.name}")
				}
			}
		}
	}
}
