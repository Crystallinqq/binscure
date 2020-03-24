package dev.binclub.binscure.processors.optimisers

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.kotlin.hasAccess
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 *
 * Enums by default store their available values in a private `values` field. This field is cloned and returned using
 * the "values()" method. Cloning this array can introduce performance and memory issues.
 *
 * This processor removes the clone call and returns the array itself.
 *
 * @author cookiedragon234 24/Mar/2020
 */
object EnumValuesOptimiser: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (classNode.access.hasAccess(ACC_ENUM)) {
				val desc = "[L${classNode.name};"
				
				val valuesMethod = classNode.methods.firstOrNull {
					it.name == "values"
					&&
					it.desc == "()$desc"
					&&
					it.instructions.size() >= 4
				} ?: return
				
				for (insn in valuesMethod.instructions) {
					if (insn is MethodInsnNode) {
						if (
							insn.opcode == INVOKEVIRTUAL
							&&
							insn.name == "clone"
						) {
							if (insn.next.opcode == CHECKCAST) {
								valuesMethod.instructions.remove(insn.next)
							}
							valuesMethod.instructions.remove(insn)
						}
					}
				}
			}
		}
	}
}
