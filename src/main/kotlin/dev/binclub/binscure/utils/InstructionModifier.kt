package dev.binclub.binscure.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import java.util.HashMap

class InstructionModifier {
	
	private val replacements = HashMap<AbstractInsnNode, InsnList>()
	private val appends = HashMap<AbstractInsnNode, InsnList>()
	private val prepends = HashMap<AbstractInsnNode, InsnList>()
	
	fun append(original: AbstractInsnNode, append: InsnList) {
		if (append.size() > 0) {
			appends[original] = append
		}
	}
	
	fun prepend(original: AbstractInsnNode, append: InsnList) {
		if (append.size() > 0) {
			prepends[original] = append
		}
	}
	
	fun replace(original: AbstractInsnNode, vararg insns: AbstractInsnNode) {
		val singleton = InsnList()
		for (replacement in insns) {
			singleton.add(replacement)
		}
		if (singleton.size() > 0) {
			replacements[original] = singleton
		}
	}
	
	fun replace(original: AbstractInsnNode, replacements: InsnList) {
		if (replacements.size() > 0) {
			this.replacements[original] = replacements
		}
	}
	
	fun remove(original: AbstractInsnNode) {
		replacements[original] = EMPTY_LIST
	}
	
	fun removeAll(toRemove: List<AbstractInsnNode>) {
		for (insn in toRemove) {
			remove(insn)
		}
	}
	
	fun apply(methodNode: MethodNode) {
		replacements.forEach { (insn, list) ->
			methodNode.instructions.insert(insn, list)
			methodNode.instructions.remove(insn)
		}
		prepends.forEach { (insn, list) -> methodNode.instructions.insertBefore(insn, list) }
		appends.forEach { (insn, list) -> methodNode.instructions.insert(insn, list) }
	}
	
	companion object {
		private val EMPTY_LIST = InsnList()
	}
}
