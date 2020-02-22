package cookiedragon.obfuscator.processors.classmerge

import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.kotlin.add
import cookiedragon.obfuscator.kotlin.clone
import cookiedragon.obfuscator.kotlin.hasAccess
import cookiedragon.obfuscator.kotlin.random
import cookiedragon.obfuscator.processors.renaming.generation.NameGenerator
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.runtime.OpaqueRuntimeManager
import cookiedragon.obfuscator.utils.getLoadForType
import cookiedragon.obfuscator.utils.getRetForType
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier

/**
 * @author cookiedragon234 21/Feb/2020
 */
object StaticMethodMerger: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		val staticMethods = hashMapOf<String, MutableSet<Pair<ClassNode, MethodNode>>>()
		
		for (classNode in classes) {
			for (method in classNode.methods) {
				if (
					method.name != "<init>"
					&&
					!method.access.hasAccess(ACC_ABSTRACT)
					&&
					!method.access.hasAccess(ACC_NATIVE)
				) {
					staticMethods.getOrPut(method.desc, { hashSetOf() }).add(Pair(classNode, method))
				}
			}
		}
		
		if (staticMethods.isNotEmpty()) {
			val namer = NameGenerator("\$o")
			
			for ((desc, methods) in staticMethods) {
				val it = methods.iterator()
				while (it.hasNext()) {
					val (firstClass, firstMethod) = it.next()
					
					val firstStatic = firstMethod.access.hasAccess(ACC_STATIC)
					val secondStatic = firstMethod.access.hasAccess(ACC_STATIC)
					
					var newNode: ClassNode
					do {
						newNode = classes.random(random)
					} while (newNode.access.hasAccess(ACC_INTERFACE))
					
					val newMethod = MethodNode(
						ACC_PUBLIC + ACC_STATIC,
						namer.uniqueRandomString(),
						firstMethod.desc.replace("(", "(Ljava/lang/Object;I"),
						null,
						null
					)
					newNode.methods.add(newMethod)
					
					val (secondClass, secondMethod) =
						(if (it.hasNext()) it.next() else Pair(null, null))
					
					newMethod.tryCatchBlocks = firstMethod.tryCatchBlocks ?: arrayListOf()
					firstMethod.tryCatchBlocks = null
					
					newMethod.localVariables = incrementLocalVars(firstMethod.localVariables?: arrayListOf(), firstStatic)
					firstMethod.localVariables = null
					
					val baseInt = random.nextInt(Integer.MAX_VALUE - 2)
					val keyInt = random.nextInt(Integer.MAX_VALUE)
					
					val firstStart = LabelNode(Label())
					val secondStart = LabelNode(Label())
					newMethod.instructions = InsnList().apply {
						val default = LabelNode(Label())
						add(default)
						add(VarInsnNode(ILOAD, 1))
						add(ldcInt(keyInt))
						add(IXOR)
						add(
							TableSwitchInsnNode(
								baseInt, baseInt + 1,
								default,
								firstStart, secondStart
							)
						)
						add(secondStart)
						if (secondMethod != null) {
							add(incAllVarInsn(secondMethod.instructions, secondStatic, secondClass!!.name))
						} else {
							InsnNode(ACONST_NULL)
							InsnNode(ATHROW)
						}
						add(firstStart)
						add(incAllVarInsn(firstMethod.instructions, firstStatic, firstClass.name))
					}
					
					firstMethod.instructions = InsnList().apply {
						val params = Type.getArgumentTypes(firstMethod.desc)
						for ((index, param) in params.withIndex()) {
							add(VarInsnNode(getLoadForType(param), index))
						}
						add(MethodInsnNode(INVOKESTATIC, newNode.name, newMethod.name, newMethod.desc))
						add(getRetForType(Type.getReturnType(firstMethod.desc)))
					}
					
					if (secondMethod != null) {
						secondMethod.instructions = firstMethod.instructions.clone()
						secondMethod.instructions.insert(
							InsnList().apply {
								if (!secondStatic) {
									add(VarInsnNode(ALOAD, 0))
								} else {
									add(ACONST_NULL)
								}
								add(ldcInt((baseInt + 1) xor keyInt))
							})
					}
					
					firstMethod.instructions.insert(InsnList().apply {
						if (!firstStatic) {
							add(VarInsnNode(ALOAD, 0))
						} else {
							add(ACONST_NULL)
						}
						add(ldcInt(baseInt xor keyInt))
					})
					
					if (secondMethod?.tryCatchBlocks != null) newMethod.tryCatchBlocks.addAll(secondMethod.tryCatchBlocks)
					secondMethod?.tryCatchBlocks = null
					if (secondMethod?.localVariables != null) newMethod.localVariables.addAll(incrementLocalVars(secondMethod.localVariables, secondStatic))
					secondMethod?.localVariables = null
				}
			}
		}
	}
	
	private fun <T: MutableCollection<LocalVariableNode>> incrementLocalVars(vars: T, static: Boolean): T {
		val toRemove = arrayListOf<LocalVariableNode>()
		for (localVar in vars) {
			if (localVar.index != 0 || static) {
				localVar.index += 1
			} else {
				toRemove.add(localVar)
			}
		}
		vars.removeAll(toRemove)
		return vars
	}
	
	private fun incAllVarInsn(insnList: InsnList, static: Boolean, classType: String): InsnList {
		return InsnList().apply {
			for (insn in insnList) {
				if (insn is VarInsnNode) {
					if (!static && insn.`var` == 0) {
						add(insn)
						add(TypeInsnNode(CHECKCAST, classType))
						continue
					} else {
						add(VarInsnNode(insn.opcode, insn.`var` + (2)))
						continue
					}
				} else if (insn is IincInsnNode) {
					add(IincInsnNode(insn.`var` + (2), insn.incr))
					continue
				}
				add(insn)
			}
		}
	}
}
