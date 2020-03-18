package dev.binclub.binscure.processors.classmerge

import dev.binclub.binscure.CObfuscator.isExcluded
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.transformers.MergeMethods
import dev.binclub.binscure.api.transformers.MergeMethods.NONE
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.kotlin.add
import dev.binclub.binscure.kotlin.clone
import dev.binclub.binscure.kotlin.hasAccess
import dev.binclub.binscure.kotlin.random
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.runtime.OpaqueRuntimeManager
import dev.binclub.binscure.utils.*
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
	private fun containsSpecial(insnList: InsnList): Boolean {
		for (insn in insnList) {
			if (insn.opcode == INVOKESPECIAL) return true
		}
		
		return false
	}
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.flowObfuscation.enabled || rootConfig.flowObfuscation.mergeMethods == NONE) {
			return
		}
		
		val staticMethods = hashMapOf<String, MutableSet<Pair<ClassNode, MethodNode>>>()
		
		for (classNode in classes) {
			for (method in classNode.methods) {
				if (
					!method.name.startsWith('<')
					&&
					!method.access.hasAccess(ACC_ABSTRACT)
					&&
					!method.access.hasAccess(ACC_NATIVE)
					//&&
					//random.nextBoolean()
				) {
					staticMethods.getOrPut(method.desc, { hashSetOf() }).add(Pair(classNode, method))
				}
			}
		}
		
		var classNode: ClassNode? = null
		
		if (staticMethods.isNotEmpty()) {
			val namer = NameGenerator("\$o")
			
			for ((desc, methods) in staticMethods) {
				val it = methods.shuffled(random).iterator()
				while (it.hasNext()) {
					val (firstClass, firstMethod) = it.next()
					
					val (secondClass, secondMethod) =
						if (it.hasNext()) it.next() else continue
					
					val firstStatic = firstMethod.access.hasAccess(ACC_STATIC)
					val secondStatic = secondMethod.access.hasAccess(ACC_STATIC)
					
					//var newNode: ClassNode
					//do {
					//	newNode = classes.random(random)
					//} while (newNode.access.hasAccess(ACC_INTERFACE) || isExcluded(newNode))
					
					if (classNode == null || classNode.methods.size >= 65530) {
						classNode = ClassNode().apply {
							access = ACC_PUBLIC
							version = classes.first().version
							name = ClassRenamer.namer.uniqueRandomString()
							superName = "java/lang/Object"
							ClassPath.classes[this.name] = this
							ClassPath.classPath[this.name] = this
						}
					}
					
					val newMethod = MethodNode(
						ACC_PUBLIC + ACC_STATIC,
						namer.uniqueRandomString(),
						firstMethod.desc.replace("(", "(Ljava/lang/Object;I"),
						null,
						null
					)
					classNode.methods.add(newMethod)
					
					newMethod.tryCatchBlocks = firstMethod.tryCatchBlocks ?: arrayListOf()
					firstMethod.tryCatchBlocks = null
					
					newMethod.localVariables = incrementLocalVars(firstMethod.localVariables?: arrayListOf(), firstStatic)
					firstMethod.localVariables = null
					
					val baseInt = random.nextInt(Integer.MAX_VALUE - 2)
					val keyInt = random.nextInt(Integer.MAX_VALUE)
					
					val firstStart = newLabel()
					val secondStart = newLabel()
					newMethod.instructions = InsnList().apply {
						val default = newLabel()
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
							add(incAllVarInsn(secondMethod.instructions, secondStatic, secondClass.name))
						} else {
							InsnNode(ACONST_NULL)
							InsnNode(ATHROW)
						}
						add(firstStart)
						add(incAllVarInsn(firstMethod.instructions, firstStatic, firstClass.name))
					}
					
					firstMethod.instructions = InsnList().apply {
						val params = getArgumentTypes(firstMethod.desc)
						for ((index, param) in params.withIndex()) {
							add(VarInsnNode(getLoadForType(param), if (firstStatic) index else (index + 1)))
						}
						add(MethodInsnNode(INVOKESTATIC, classNode.name, newMethod.name, newMethod.desc))
						add(getRetForType(getReturnType(firstMethod.desc)))
					}
					
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
					
					firstMethod.instructions.insert(InsnList().apply {
						if (!firstStatic) {
							add(VarInsnNode(ALOAD, 0))
						} else {
							add(ACONST_NULL)
						}
						add(ldcInt(baseInt xor keyInt))
					})
					
					if (secondMethod.tryCatchBlocks != null) newMethod.tryCatchBlocks.addAll(secondMethod.tryCatchBlocks)
					secondMethod.tryCatchBlocks = null
					if (secondMethod.localVariables != null) newMethod.localVariables.addAll(incrementLocalVars(secondMethod.localVariables, secondStatic))
					secondMethod.localVariables = null
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
				//toRemove.add(localVar)
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
