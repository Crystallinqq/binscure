@file:Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection", "unused")

package dev.binclub.binscure.utils

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode

/**
 * @author cookiedragon234 18/May/2020
 */
fun insnBuilder(application: InsnBuilder.() -> Unit): InsnList {
	return InsnBuilder().apply(application).list
}

@Suppress("FunctionName")
class InsnBuilder {
	val list = InsnList()
	
	inline operator fun InsnList.unaryPlus() = list.add(this)
	inline operator fun AbstractInsnNode.unaryPlus() = list.add(this)
	inline fun Int.insn() = InsnNode(this)
	
	inline fun insn(opcode: Int) = +InsnNode(opcode)
	inline fun pop() = insn(POP)
	inline fun ineg() = insn(INEG)
	inline fun isub() = insn(ISUB)
	inline fun iadd() = insn(IADD)
	inline fun imul() = insn(IMUL)
	inline fun ior() = insn(IOR)
	inline fun iand() = insn(IAND)
	inline fun ixor() = insn(IXOR)
	inline fun swap() = insn(SWAP)
	inline fun dup() = insn(DUP)
	inline fun dup_x1() = insn(DUP_X1)
	inline fun dup_x2() = insn(DUP_X2)
	inline fun dup2() = insn(DUP2)
	inline fun iconst_1() = insn(ICONST_1)
	inline fun iconst_m1() = insn(ICONST_M1)
}
