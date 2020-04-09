package dev.binclub.binscure.utils

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author cookiedragon234 24/Feb/2020
 */
class VariableAnalyser(val methodNode: MethodNode) {
	companion object {
		private val objectType = Type.getType(Any::class.java)
	}
	
	fun getNextIndex(): Int {
		var max = -1
		
		if (methodNode.access.hasAccess(ACC_STATIC)) {
			max += 1
		}
		
		methodNode.localVariables?.also {
			for (localVar in it) {
				if (localVar.index > max) max = localVar.index
			}
		}
		
		methodNode.instructions?.also {
			for (insn in it) {
				if (insn is VarInsnNode) {
					if (insn.`var` > max) max = insn.`var`
				}
			}
		}
		
		return max + 1
	}
	
	
	private fun typeFromLoad(insn: VarInsnNode): Type {
		return when (insn.opcode) {
			ILOAD -> Type.INT_TYPE
			LLOAD -> Type.LONG_TYPE
			FLOAD -> Type.FLOAT_TYPE
			DLOAD -> Type.DOUBLE_TYPE
			ALOAD -> objectType
			else -> throw IllegalArgumentException("Unprepared opcode ${insn.opcodeString()}")
		}
	}
}
