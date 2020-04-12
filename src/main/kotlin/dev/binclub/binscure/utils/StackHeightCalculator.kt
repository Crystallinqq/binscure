package dev.binclub.binscure.utils

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*
import kotlin.collections.HashMap

/**
 * @author cookiedragon234 12/Apr/2020
 */
fun calculateStackHeight(methodNode: MethodNode) = calculateStackHeight(methodNode.instructions, methodNode.tryCatchBlocks)

fun calculateStackHeight(insnList: InsnList, tryCatchBlockNodes: Collection<TryCatchBlockNode>): Map<AbstractInsnNode, Collection<StackInfo>> {
	var insn: AbstractInsnNode? = insnList.first
	val out = hashMapOf<AbstractInsnNode, MutableList<StackInfo>>()
	
	val stackInfo = StackInfo(
		Stack(),
		hashMapOf()
	)
	
	while (insn != null) {
		when (insn) {
			is VarInsnNode -> {
				val info = varInfoMap[insn.opcode] ?: error("Unexpected opcode ${insn.opcode}")
				
				if (info.stores) {
					stackInfo.registers[insn.`var`] = stackInfo.stack.pop()
				} else {
					stackInfo.stack.push(info.type)
				}
			}
			is LdcInsnNode -> {
			
			}
		}
		stackAtInsn(insn, stackInfo.stack, stackInfo.registers)
		out.getOrPutLazy(insn, {
			arrayListOf()
		}).add(stackInfo)
		
		insn = insn.next
	}
	
	return out
}

fun stackAtInsn(insn: AbstractInsnNode, stack: Stack<Type>, registers: MutableMap<Int, Type>) {
	when (insn) {
		is VarInsnNode -> {
			val info = varInfoMap[insn.opcode] ?: error("Unexpected opcode ${insn.opcode}")
			
			if (info.stores) {
				registers[insn.`var`] = stack.pop()
			} else {
				stack.push(info.type)
			}
		}
		is LdcInsnNode -> {
		
		}
	}
}

data class StackInfo(
	val stack: Stack<Type>,
	val registers: HashMap<Int, Type>
) {
	fun clone(): StackInfo {
		return StackInfo(stack.clone() as Stack<Type>, registers.clone() as HashMap<Int, Type>)
	}
}

val OBJECT_TYPE = Type.getObjectType("java/lang/Object")

data class VarInsnInfo(
	val stores: Boolean,
	val type: Type
)
private val varInfoMap = hashMapOf(
	ILOAD to VarInsnInfo(false, Type.INT_TYPE),
	LLOAD to VarInsnInfo(false, Type.LONG_TYPE),
	FLOAD to VarInsnInfo(false, Type.FLOAT_TYPE),
	DLOAD to VarInsnInfo(false, Type.DOUBLE_TYPE),
	ALOAD to VarInsnInfo(false, OBJECT_TYPE),
	ISTORE to VarInsnInfo(true, Type.INT_TYPE),
	LSTORE to VarInsnInfo(true, Type.LONG_TYPE),
	FSTORE to VarInsnInfo(true, Type.FLOAT_TYPE),
	DSTORE to VarInsnInfo(true, Type.DOUBLE_TYPE),
	ASTORE to VarInsnInfo(true, OBJECT_TYPE)
)
