package dev.binclub.binscure.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

class InstructionModifier {
	private val replacements = hashMapOf<AbstractInsnNode, InsnList>()
	private val appends = hashMapOf<AbstractInsnNode, InsnList>()
	private val prepends = hashMapOf<AbstractInsnNode, InsnList>()
	
	fun append(original: AbstractInsnNode, vararg appends: AbstractInsnNode) = append(original, insnListOf(*appends))
	
	fun append(original: AbstractInsnNode, append: InsnList) {
		if (append.size() > 0) {
			appends[original] = append
		}
	}
	
	fun prepend(original: AbstractInsnNode, vararg prepends: AbstractInsnNode) = prepend(original, insnListOf(*prepends))
	
	fun prepend(original: AbstractInsnNode, prepend: InsnList) {
		if (prepend.size() > 0) {
			prepends[original] = prepend
		}
	}
	
	fun replace(original: AbstractInsnNode, vararg replacements: AbstractInsnNode) = replace(original, insnListOf(*replacements))
	
	fun replace(original: AbstractInsnNode, replacements: InsnList) {
		if (replacements.size() > 0) {
			this.replacements[original] = replacements
		}
	}
	
	fun remove(original: AbstractInsnNode) {
		replacements[original] = EMPTY_LIST
	}
	
	fun apply(methodNode: MethodNode) = apply(methodNode.instructions)
	
	fun apply(instructions: InsnList) {
		for ((insn, list) in replacements) {
			instructions.insert(insn, list)
			instructions.remove(insn)
		}
		for ((insn, list) in appends) { instructions.insert(insn, list) }
		for ((insn, list) in prepends) { instructions.insertBefore(insn, list) }
	}
	
	companion object {
		private val EMPTY_LIST = InsnList()
	}
}
