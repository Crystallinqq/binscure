package dev.binclub.binscure.processors

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import kotlin.collections.getOrPut

/**
 * @author cookiedragon234 14/Apr/2020
 */
object VariableInitializer: IClassProcessor {
	override val progressDescription: String
		get() = "Initialising variables"
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method) || method.instructions.first == null)
					continue
				
				// First pass, seperate variable types into different slots
				block {
					val varMap = mutableMapOf<Int, Int>()
					
					var start = 0
					if (!method.access.hasAccess(ACC_STATIC)) {
						varMap[0] = Type.OBJECT
						start = 1
					}
					
					for (arg in Type.getArgumentTypes(method.desc)) {
						varMap[start] = arg.sort
						start += if (arg.doubleSize) 2 else 1
					}
					
					for (insn in method.instructions) {
						if (insn is VarInsnNode) {
							if (insn.`var` > start) {
								start = insn.`var`
							}
						}
					}
					
					class Replacement(
						val type: Int,
						val newIndex: Int
					)
					val replacements = hashMapOf<Int, MutableList<Replacement>>()
					
					for (insn in method.instructions) {
						if (insn is VarInsnNode) {
							val type = typeFromVarOp(insn.opcode)
							
							val oldType = varMap[insn.`var`]
							
							replacements[insn.`var`]?.let {
								for (replacement in it) {
									if (replacement.type == type) {
										continue
									}
								}
							}
							
							if (oldType != null && oldType != type) {
								start += if (type == Type.DOUBLE || type == Type.LONG) 2 else 1
								
								replacements.getOrPut(insn.`var`, { arrayListOf()}).add(Replacement(type, start))
							} else {
								varMap[insn.`var`] = type
							}
						}
					}
					
					for (insn in method.instructions) {
						if (insn is VarInsnNode) {
							replacements[insn.`var`]?.let {
								for (replacement in it) {
									val type = typeFromVarOp(insn.opcode)
									if (replacement.type == type) {
										insn.`var` = replacement.newIndex
									}
								}
							}
						}
					}
				}
				
				block {
					val modifier = InstructionModifier()
					val varMap = mutableMapOf<Int, Int>()
					var lastFrame: FrameNode? = null
					for (insn in method.instructions) {
						if (insn is FrameNode) {
							lastFrame = insn
						} else if (insn is VarInsnNode) {
							if (insn.opcode == ALOAD && lastFrame != null) {
								val frame = lastFrame
								if (frame.local.size > insn.`var` && frame.local[insn.`var`] is String) {
									modifier.append(insn, TypeInsnNode(CHECKCAST, frame.local[insn.`var`] as String))
								}
							}
							varMap[insn.`var`] = when (insn.opcode) {
								ILOAD -> ISTORE
								LLOAD -> LSTORE
								FLOAD -> FSTORE
								DLOAD -> DSTORE
								ALOAD -> ASTORE
								else -> insn.opcode
							}
						}
					}
					
					var highestArg = if (method.access.hasAccess(ACC_STATIC)) -1 else 0
					for (arg in Type.getArgumentTypes(method.desc)) {
						highestArg += if (arg.doubleSize) 2 else 1
					}
					val prepend = InsnList().apply {
						for ((index, opcode) in varMap) {
							if (index <= highestArg) {
								continue
							}
							add(ldcFromStore(opcode))
							add(VarInsnNode(opcode, index))
						}
					}
					
					modifier.prepend(method.instructions.first, prepend)
					modifier.apply(method)
				}
			}
		}
	}
}
/*
object VariableInitializerStage2: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method) || method.instructions.first == null)
					continue
				
				val modifier = InstructionModifier()
				var lastFrame: FrameNode? = null
				for (insn in method.instructions) {
					if (insn is FrameNode) {
						lastFrame = insn
					} else if (insn is VarInsnNode) {
						if (insn.opcode == ALOAD && lastFrame != null) {
							val frame = lastFrame
							if (frame.local.size > insn.`var` && frame.local[insn.`var`] is String) {
								val type = frame.local[insn.`var`]
								if ()
								modifier.append(insn, TypeInsnNode(CHECKCAST, frame.local[insn.`var`] as String))
							}
						}
					}
				}
				modifier.apply(method)
			}
		}
	}
}*/

fun ldcFromStore(opcode: Int): AbstractInsnNode = InsnNode(when (opcode) {
	ISTORE -> ICONST_0
	LSTORE -> LCONST_0
	FSTORE -> FCONST_0
	DSTORE -> DCONST_0
	ASTORE -> ACONST_NULL
	else -> error("Illegal store $opcode")
})

fun typeFromVarOp(opcode: Int): Int = when (opcode) {
	ILOAD -> Type.INT
	LLOAD -> Type.LONG
	FLOAD -> Type.FLOAT
	DLOAD -> Type.DOUBLE
	ALOAD -> Type.OBJECT
	ISTORE -> Type.INT
	LSTORE -> Type.LONG
	FSTORE -> Type.FLOAT
	DSTORE -> Type.DOUBLE
	ASTORE -> Type.OBJECT
	else -> error("Unexpected opcode ${opcode}")
}
