@file:Suppress("NOTHING_TO_INLINE", "SpellCheckingInspection", "unused")

package dev.binclub.binscure.utils

import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

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
	
	inline fun _return() = insn(RETURN)
	inline fun areturn() = insn(ARETURN)
	
	inline fun aconst_null() = insn(ACONST_NULL)
	
	inline fun pop() = insn(POP)
	inline fun athrow() = insn(ATHROW)
	
	inline fun ineg() = insn(INEG)
	inline fun isub() = insn(ISUB)
	inline fun iadd() = insn(IADD)
	inline fun imul() = insn(IMUL)
	inline fun ior() = insn(IOR)
	inline fun iand() = insn(IAND)
	inline fun ixor() = insn(IXOR)
	
	inline fun i2f() = insn(I2F)
	
	inline fun swap() = insn(SWAP)
	
	inline fun dup() = insn(DUP)
	inline fun dup_x1() = insn(DUP_X1)
	inline fun dup_x2() = insn(DUP_X2)
	inline fun dup2() = insn(DUP2)
	
	inline fun iconst_m1() = insn(ICONST_M1)
	inline fun iconst_1() = insn(ICONST_1)
	inline fun iconst_2() = insn(ICONST_2)
	inline fun iconst_3() = insn(ICONST_3)
	inline fun iconst_4() = insn(ICONST_4)
	
	inline fun goto(labelNode: LabelNode) = +JumpInsnNode(GOTO, labelNode)
	inline fun ifeq(labelNode: LabelNode) = +JumpInsnNode(IFEQ, labelNode)
	inline fun ifne(labelNode: LabelNode) = +JumpInsnNode(IFNE, labelNode)
	inline fun if_icmpeq(labelNode: LabelNode) = +JumpInsnNode(IF_ICMPEQ, labelNode)
	inline fun ifnull(labelNode: LabelNode) = +JumpInsnNode(IFNULL, labelNode)
	inline fun ifnonnull(labelNode: LabelNode) = +JumpInsnNode(IFNONNULL, labelNode)
	
	inline fun astore(`var`: Int) = +VarInsnNode(ASTORE, `var`)
	inline fun aload(`var`: Int) = +VarInsnNode(ALOAD, `var`)
	inline fun iload(`var`: Int) = +VarInsnNode(ILOAD, `var`)
	inline fun istore(`var`: Int) = +VarInsnNode(ISTORE, `var`)
	inline fun fload(`var`: Int) = +VarInsnNode(FLOAD, `var`)
	inline fun fstore(`var`: Int) = +VarInsnNode(FSTORE, `var`)
	
	inline fun aastore() = insn(AASTORE)
	
	inline fun arraylength() = insn(ARRAYLENGTH)
	
	inline fun invokestatic(owner: String, name: String, desc: String, `interface`: Boolean = false)
	= +MethodInsnNode(INVOKESTATIC, owner, name, desc, `interface`)
	inline fun invokevirtual(owner: String, name: String, desc: String, `interface`: Boolean = false)
	= +MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, `interface`)
	inline fun invokespecial(owner: String, name: String, desc: String, `interface`: Boolean = false)
	= +MethodInsnNode(INVOKESPECIAL, owner, name, desc, `interface`)
	inline fun getstatic(owner: String, name: String, desc: String)
	= +FieldInsnNode(GETSTATIC, owner, name, desc)
	
	inline fun new(type: String) = +TypeInsnNode(NEW, type)
	inline fun newboolarray() = newarray(T_BOOLEAN)
	inline fun newchararray() = newarray(T_CHAR)
	inline fun newbytearray() = newarray(T_BYTE)
	inline fun newshortarray()  = newarray(T_SHORT)
	inline fun newintarray() = newarray(T_INT)
	inline fun newfloatarray() = newarray(T_FLOAT)
	inline fun newdoublearray() = newarray(T_DOUBLE)
	inline fun newlongarray() = newarray(T_LONG)
	inline fun newarray(type: Int) = +IntInsnNode(NEWARRAY, type)
	inline fun anewarray(desc: String) = +TypeInsnNode(ANEWARRAY, desc)
	
	inline fun ldc(`val`: Int) = +ldcInt(`val`)
	inline fun ldc(`val`: Float) = +ldcFloat(`val`)
	inline fun ldc(`val`: Long) = +ldcLong(`val`)
	inline fun ldc(`val`: Double) = +ldcDouble(`val`)
	inline fun ldc(`val`: String) = +LdcInsnNode(`val`)
	inline fun ldc(`val`: Type) = +LdcInsnNode(`val`)
	inline fun ldc(`val`: Handle) = +LdcInsnNode(`val`)
	inline fun ldc(`val`: ConstantDynamic) = +LdcInsnNode(`val`)
}
