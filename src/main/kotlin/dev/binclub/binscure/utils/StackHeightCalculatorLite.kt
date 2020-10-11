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
		try {
			val out = HashMap<AbstractInsnNode, Int>(mn.instructions?.size() ?: 0)
			
			mn.tryCatchBlocks?.forEach {
				calculate(it.handler, out, 1)
			}
			
			val first = mn.instructions?.first ?: return out
			calculate(first, out, 0)
			
			return out
		} catch (ex: StackHeightCalculationException) {
			File("stackheightlog.txt").printWriter().use {
				it.println("--- ${mn.instructions.size()}:")
				it.println(mn.instructions.toOpcodeStrings(ex.insn))
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
				LCONST_0 -> 1
				LCONST_1 -> 1
				FCONST_0 -> 1
				FCONST_1 -> 1
				FCONST_2 -> 1
				DCONST_0 -> 1
				DCONST_1 -> 1
				BIPUSH -> 1
				SIPUSH -> 1
				LDC -> 1
				ILOAD -> 1
				LLOAD -> 1
				FLOAD -> 1
				DLOAD -> 1
				ALOAD -> 1
				IALOAD -> 1 - 2
				LALOAD -> 1 - 2
				FALOAD -> 1 - 2
				DALOAD -> 1 - 2
				AALOAD -> 1 - 2
				BALOAD -> 1 - 2
				CALOAD -> 1 - 2
				SALOAD -> 1 - 2
				ISTORE -> -1
				LSTORE -> -1
				FSTORE -> -1
				DSTORE -> -1
				ASTORE -> -1
				IASTORE -> -3
				LASTORE -> -3
				FASTORE -> -3
				DASTORE -> -3
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
				LADD -> 1 - 2
				FADD -> 1 - 2
				DADD -> 1 - 2
				ISUB -> 1 - 2
				LSUB -> 1 - 2
				FSUB -> 1 - 2
				DSUB -> 1 - 2
				IMUL -> 1 - 2
				LMUL -> 1 - 2
				FMUL -> 1 - 2
				DMUL -> 1 - 2
				IDIV -> 1 - 2
				LDIV -> 1 - 2
				FDIV -> 1 - 2
				DDIV -> 1 - 2
				IREM -> 1 - 2
				LREM -> 1 - 2
				FREM -> 1 - 2
				DREM -> 1 - 2
				INEG -> 0
				LNEG -> 0
				FNEG -> 0
				DNEG -> 0
				ISHL -> 1 - 2
				LSHL -> 1 - 2
				ISHR -> 1 - 2
				LSHR -> 1 - 2
				IUSHR -> 1 - 2
				LUSHR -> 1 - 2
				IAND -> 1 - 2
				LAND -> 1 - 2
				IOR -> 1 - 2
				LOR -> 1 - 2
				IXOR -> 1 - 2
				LXOR -> 1 - 2
				IINC -> 0
				I2L -> 0
				I2F -> 0
				I2D -> 0
				L2I -> 0
				L2F -> 0
				L2D -> 0
				F2I -> 0
				F2L -> 0
				F2D -> 0
				D2I -> 0
				D2L -> 0
				D2F -> 0
				I2B -> 0
				I2C -> 0
				I2S -> 0
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
				GETSTATIC -> 1
				PUTSTATIC -> -1
				GETFIELD -> 0
				PUTFIELD -> -2
				INVOKEVIRTUAL -> {
					insn as MethodInsnNode
					val ret = if (Type.getReturnType(insn.desc) == Type.VOID_TYPE) 0 else 1
					ret - 1 - Type.getArgumentTypes(insn.desc).size
				}
				INVOKESPECIAL -> {
					insn as MethodInsnNode
					val ret = if (Type.getReturnType(insn.desc) == Type.VOID_TYPE) 0 else 1
					ret - 1 - Type.getArgumentTypes(insn.desc).size
				}
				INVOKESTATIC -> {
					insn as MethodInsnNode
					val ret = if (Type.getReturnType(insn.desc) == Type.VOID_TYPE) 0 else 1
					ret - Type.getArgumentTypes(insn.desc).size
				}
				INVOKEINTERFACE -> {
					insn as MethodInsnNode
					val ret = if (Type.getReturnType(insn.desc) == Type.VOID_TYPE) 0 else 1
					ret - 1 - Type.getArgumentTypes(insn.desc).size
				}
				INVOKEDYNAMIC -> {
					insn as InvokeDynamicInsnNode
					val ret = if (Type.getReturnType(insn.desc) == Type.VOID_TYPE) 0 else 1
					ret - Type.getArgumentTypes(insn.desc).size
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
}
