package dev.binclub.binscure.utils

import dev.binclub.binscure.processors.arithmetic.MbaTransformer
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

class InstructionModifier {
	private val replacements = arrayListOf<ReplaceOp>()
	private val appends = arrayListOf<AppendOp>()
	private val prepends = arrayListOf<PrependOp>()
	
	fun append(original: AbstractInsnNode, vararg appends: AbstractInsnNode) = append(original, insnListOf(*appends))
	
	fun append(original: AbstractInsnNode, append: InsnList) {
		appends += AppendOp(original, append)
	}
	
	fun prepend(original: AbstractInsnNode, vararg prepends: AbstractInsnNode) = prepend(original, insnListOf(*prepends))
	
	fun prepend(original: AbstractInsnNode, prepend: InsnList) {
		prepends += PrependOp(original, prepend)
	}
	
	fun replace(original: AbstractInsnNode, vararg replacements: AbstractInsnNode) = replace(original, insnListOf(*replacements))
	
	fun remove(original: AbstractInsnNode) = replace(original, EMPTY_LIST)
	
	fun replace(original: AbstractInsnNode, replace: InsnList) {
		replacements += ReplaceOp(original, replace)
	}
	
	fun apply(methodNode: MethodNode) = apply(methodNode.instructions)
	
	fun apply(instructions: InsnList, maxInsns: Int? = null): Boolean {
		replacements.forEach { replacement ->
			if (maxInsns != null && instructions.size()> maxInsns) return false
			replacement.apply(instructions)
		}
		appends.forEach { append ->
			if (maxInsns != null && instructions.size()> maxInsns) return false
			append.apply(instructions)
		}
		prepends.forEach { prepend ->
			if (maxInsns != null && instructions.size()> maxInsns) return false
			prepend.apply(instructions)
		}
		return true
	}
	
	fun isEmpty(): Boolean = this.replacements.isEmpty() && this.prepends.isEmpty() && this.appends.isEmpty()
	
	interface Op {
		fun apply(instructions: InsnList)
	}
	
	class ReplaceOp (val insn: AbstractInsnNode, val list: InsnList): Op {
		override fun apply(instructions: InsnList) {
			instructions.insert(insn, list)
			instructions.remove(insn)
		}
	}
	
	class AppendOp (val insn: AbstractInsnNode, val list: InsnList): Op {
		override fun apply(instructions: InsnList) {
			instructions.insert(insn, list)
		}
	}
	
	class PrependOp (val insn: AbstractInsnNode, val list: InsnList): Op {
		override fun apply(instructions: InsnList) {
			instructions.insertBefore(insn, list)
		}
	}
	
	companion object {
		private val EMPTY_LIST = InsnList()
	}
}
