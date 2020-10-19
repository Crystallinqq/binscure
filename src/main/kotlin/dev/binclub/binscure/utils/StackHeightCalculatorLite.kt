@file:Suppress("NOTHING_TO_INLINE")

package dev.binclub.binscure.utils

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.io.File

/**
 * @author cookiedragon234 24/Sep/2020
 */
object StackHeightCalculatorLite {
	fun calculate(mn: MethodNode): HashMap<AbstractInsnNode, Int> {
		val out = HashMap<AbstractInsnNode, Int>(mn.instructions?.size() ?: 0)
		try {
			mn.tryCatchBlocks?.forEach {
				calculate(it.handler, out, 1)
			}
			
			val first = mn.instructions?.first ?: return out
			calculate(first, out, 0)
			
			return out
		} catch (ex: StackHeightCalculationException) {
			File("stackheightlog.txt").printWriter().use {
				it.println("--- ${mn.instructions.size()}:")
				it.println(mn.instructions.toOpcodeStrings(ex.insn, out))
				it.println("---")
			}
			println("\rStackHeightCalculation error! Written a detailed log to stackheightlog.txt")
			
			throw ex
		}
	}
	
	fun calculate(insn: AbstractInsnNode, heights: HashMap<AbstractInsnNode, Int>, height: Int = 0) {
		var height = height
		var insn: AbstractInsnNode? = insn
		
		insnLoop@while (true) {
			if (insn == null) break
			if (insn in heights) {
				// this path has already been traced
				break
			}
			
			if (height < 0) {
				throw StackHeightCalculationException(insn, IndexOutOfBoundsException("Stack Underflow"))
			}
			heights[insn] = height
			
			height += when (insn.opcode) {
				-1 -> 0
				NOP -> 0
				ACONST_NULL -> 1
				ICONST_M1 -> 1
				ICONST_0 -> 1
				ICONST_1 -> 1
				ICONST_2 -> 1
				ICONST_3 -> 1
				ICONST_4 -> 1
				ICONST_5 -> 1
				LCONST_0 -> 2
				LCONST_1 -> 2
				FCONST_0 -> 1
				FCONST_1 -> 1
				FCONST_2 -> 1
				DCONST_0 -> 2
				DCONST_1 -> 2
				BIPUSH -> 1
				SIPUSH -> 1
				LDC -> {
					when ((insn as LdcInsnNode).cst) {
						is Double -> 2
						is Long -> 2
						else -> 1
					}
				}
				ILOAD -> 1
				LLOAD -> 2
				FLOAD -> 1
				DLOAD -> 2
				ALOAD -> 1
				IALOAD -> 1 - 2
				LALOAD -> 2 - 2
				FALOAD -> 1 - 2
				DALOAD -> 2 - 2
				AALOAD -> 1 - 2
				BALOAD -> 1 - 2
				CALOAD -> 1 - 2
				SALOAD -> 1 - 2
				ISTORE -> -1
				LSTORE -> -2
				FSTORE -> -1
				DSTORE -> -2
				ASTORE -> -1
				IASTORE -> -3
				LASTORE -> -4
				FASTORE -> -3
				DASTORE -> -4
				AASTORE -> -3
				BASTORE -> -3
				CASTORE -> -3
				SASTORE -> -3
				POP -> -1
				POP2 -> -2
				DUP -> 1
				DUP_X1 -> 1
				DUP_X2 -> 1
				DUP2 -> 2
				DUP2_X1 -> 2
				DUP2_X2 -> 2
				SWAP -> 0
				IADD -> 1 - 2
				LADD -> 2 - 4
				FADD -> 1 - 2
				DADD -> 2 - 4
				ISUB -> 1 - 2
				LSUB -> 2 - 4
				FSUB -> 1 - 2
				DSUB -> 2 - 4
				IMUL -> 1 - 2
				LMUL -> 2 - 4
				FMUL -> 1 - 2
				DMUL -> 2 - 4
				IDIV -> 1 - 2
				LDIV -> 2 - 4
				FDIV -> 1 - 2
				DDIV -> 2 - 4
				IREM -> 1 - 2
				LREM -> 2 - 4
				FREM -> 1 - 2
				DREM -> 2 - 4
				INEG -> 0
				LNEG -> 0
				FNEG -> 0
				DNEG -> 0
				ISHL -> 1 - 2
				LSHL -> 2 - 3
				ISHR -> 1 - 2
				LSHR -> 2 - 3
				IUSHR -> 1 - 2
				LUSHR -> 2 - 3
				IAND -> 1 - 2
				LAND -> 2 - 4
				IOR -> 1 - 2
				LOR -> 2 - 4
				IXOR -> 1 - 2
				LXOR -> 2 - 4
				IINC -> 1 - 1
				I2L -> 2 - 1
				I2F -> 1 - 1
				I2D -> 2 - 1
				L2I -> 1 - 2
				L2F -> 1 - 2
				L2D -> 2 - 2
				F2I -> 1 - 1
				F2L -> 2 - 1
				F2D -> 2 - 1
				D2I -> 1 - 2
				D2L -> 2 - 2
				D2F -> 1 - 2
				I2B -> 1 - 1
				I2C -> 1 - 1
				I2S -> 1 - 1
				LCMP -> 1 - 2
				FCMPL -> 1 - 2
				FCMPG -> 1 - 2
				DCMPL -> 1 - 2
				DCMPG -> 1 - 2
				IFEQ -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IFNE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IFLT -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IFGE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IFGT -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IFLE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IF_ICMPEQ -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ICMPNE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ICMPLT -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ICMPGE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ICMPGT -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ICMPLE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ACMPEQ -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				IF_ACMPNE -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 2)
					-2
				}
				GOTO -> {
					insn as JumpInsnNode
					insn = insn.label!!
					continue@insnLoop
				}
				JSR -> error("Unsupported JSR")
				RET -> error("Unsupported RET")
				TABLESWITCH -> {
					insn as TableSwitchInsnNode
					
					insn.labels.forEach { lbl ->
						calculate(lbl, heights, height - 1)
					}
					
					insn = insn.dflt!!
					-1
				}
				LOOKUPSWITCH -> {
					insn as LookupSwitchInsnNode
					
					insn.labels.forEach { lbl ->
						calculate(lbl, heights, height - 1)
					}
					
					insn = insn.dflt!!
					-1
				}
				IRETURN -> break@insnLoop
				LRETURN -> break@insnLoop
				FRETURN -> break@insnLoop
				DRETURN -> break@insnLoop
				ARETURN -> break@insnLoop
				RETURN -> break@insnLoop
				GETSTATIC -> {
					insn as FieldInsnNode
					typeSize(Type.getType(insn.desc)) - 0
				}
				GETFIELD -> {
					insn as FieldInsnNode
					typeSize(Type.getType(insn.desc)) - 1
				}
				PUTSTATIC -> {
					insn as FieldInsnNode
					-0 - typeSize(Type.getType(insn.desc))
				}
				PUTFIELD -> {
					insn as FieldInsnNode
					-1 - typeSize(Type.getType(insn.desc))
				}
				INVOKEVIRTUAL -> {
					insn as MethodInsnNode
					val ret = typeSize(Type.getReturnType(insn.desc))
					ret - 1 - argsSize(Type.getArgumentTypes(insn.desc))
				}
				INVOKESPECIAL -> {
					insn as MethodInsnNode
					val ret = typeSize(Type.getReturnType(insn.desc))
					ret - 1 - argsSize(Type.getArgumentTypes(insn.desc))
				}
				INVOKESTATIC -> {
					insn as MethodInsnNode
					val ret = typeSize(Type.getReturnType(insn.desc))
					ret - argsSize(Type.getArgumentTypes(insn.desc))
				}
				INVOKEINTERFACE -> {
					insn as MethodInsnNode
					val ret = typeSize(Type.getReturnType(insn.desc))
					ret - 1 - argsSize(Type.getArgumentTypes(insn.desc))
				}
				INVOKEDYNAMIC -> {
					insn as InvokeDynamicInsnNode
					val ret = typeSize(Type.getReturnType(insn.desc))
					ret - argsSize(Type.getArgumentTypes(insn.desc))
				}
				NEW -> 1
				NEWARRAY -> 0
				ANEWARRAY -> 0
				ARRAYLENGTH -> 0
				ATHROW -> break@insnLoop
				CHECKCAST -> 0
				INSTANCEOF -> 0
				MONITORENTER -> -1
				MONITOREXIT -> -1
				MULTIANEWARRAY -> {
					insn as MultiANewArrayInsnNode
					1 - insn.dims
				}
				IFNULL -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				IFNONNULL -> {
					insn as JumpInsnNode
					calculate(insn.label, heights, height - 1)
					-1
				}
				else -> {
					println("Unkown opcode ${insn.opcode}")
					0
				}
			}
			
			insn = insn.next
		}
	}
	
	private fun argsSize(args: Array<Type>): Int {
		var out = 0
		args.forEach { arg ->
			out += typeSize(arg)
		}
		return out
	}
	
	private inline fun typeSize(type: Type): Int
		= when {
			type == Type.VOID_TYPE -> 0
			type.doubleSize -> 2
			else -> 1
		}
}
