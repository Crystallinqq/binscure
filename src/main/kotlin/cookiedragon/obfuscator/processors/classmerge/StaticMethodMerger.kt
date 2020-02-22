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
					method.access.hasAccess(ACC_STATIC)
					&&
					!method.access.hasAccess(ACC_ABSTRACT)
					&&
					!method.access.hasAccess(ACC_NATIVE)
				) {
					staticMethods.getOrPut(method.desc, { hashSetOf() }).add(Pair(classNode, method))
				} else {
					println(Modifier.isStatic(method.access))
					println(Modifier.isAbstract(method.access))
					println(Modifier.isNative(method.access))
				}
			}
		}
		
		if (staticMethods.isNotEmpty()) {/*
			val newNode = ClassNode().apply {
				this.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
				this.version = OpaqueRuntimeManager.classes.first().version
				this.name = ClassRenamer.namer.uniqueRandomString()
				this.signature = null
				this.superName = "java/lang/Object"
				OpaqueRuntimeManager.classes.add(this)
				ClassPath.classes[this.name] = this
				ClassPath.classPath[this.name] = this
			}*/
			
			val namer = NameGenerator("\$o")
			
			for ((desc, methods) in staticMethods) {
				val it = methods.iterator()
				while (it.hasNext()) {
					val (firstClass, firstMethod) = it.next()
					
					var newNode: ClassNode
					do {
						newNode = classes.random(random)
					} while (newNode.access.hasAccess(ACC_INTERFACE))
					
					val newMethod = MethodNode(
						ACC_PUBLIC + ACC_STATIC,
						namer.uniqueRandomString(),
						if (it.hasNext()) firstMethod.desc.replace("(", "(I") else firstMethod.desc,
						null,
						null)
					newNode.methods.add(newMethod)
					
					if (it.hasNext()) {
						val (secondClass, secondMethod) = it.next()
						
						newMethod.tryCatchBlocks = firstMethod.tryCatchBlocks ?: arrayListOf()
						firstMethod.tryCatchBlocks = null
						
						newMethod.localVariables = firstMethod.localVariables ?: arrayListOf()
						firstMethod.localVariables = null
						
						val baseInt = random.nextInt(Integer.MAX_VALUE - 2)
						val keyInt = random.nextInt(Integer.MAX_VALUE)
						
						val firstStart = LabelNode(Label())
						val secondStart = LabelNode(Label())
						newMethod.instructions = InsnList().apply {
							val default = LabelNode(Label())
							add(default)
							add(VarInsnNode(ILOAD, 0))
							add(ldcInt(keyInt))
							add(IXOR)
							add(TableSwitchInsnNode(
								baseInt, baseInt + 1,
								default,
								firstStart, secondStart
							))
							add(secondStart)
							add(incAllVarInsn(secondMethod.instructions))
							add(firstStart)
							add(incAllVarInsn(firstMethod.instructions))
						}
						
						firstMethod.instructions = InsnList().apply {
							val params = Type.getArgumentTypes(firstMethod.desc)
							for ((index, param) in params.withIndex()) {
								add(VarInsnNode(getLoadForType(param), index))
							}
							add(MethodInsnNode(INVOKESTATIC, newNode.name, newMethod.name, newMethod.desc))
							add(getRetForType(Type.getReturnType(firstMethod.desc)))
						}
						secondMethod.instructions = firstMethod.instructions.clone()
						
						firstMethod.instructions.insert(ldcInt(baseInt xor keyInt))
						secondMethod.instructions.insert(ldcInt((baseInt + 1) xor keyInt))
							
						if (secondMethod.tryCatchBlocks != null) newMethod.tryCatchBlocks.addAll(secondMethod.tryCatchBlocks)
						secondMethod.tryCatchBlocks = null
						if (secondMethod.localVariables != null) newMethod.localVariables.addAll(secondMethod.localVariables)
						secondMethod.localVariables = null
						
						for (localVariable in newMethod.localVariables) {
							localVariable.index += 1
						}
					} else {
						newMethod.tryCatchBlocks = firstMethod.tryCatchBlocks
						firstMethod.tryCatchBlocks = null
						
						newMethod.localVariables = firstMethod.localVariables
						firstMethod.localVariables = null
						
						newMethod.instructions = firstMethod.instructions.clone()
						firstMethod.instructions = InsnList().apply {
							val params = Type.getArgumentTypes(firstMethod.desc)
							for ((index, param) in params.withIndex()) {
								add(VarInsnNode(getLoadForType(param), index))
							}
							add(MethodInsnNode(INVOKESTATIC, newNode.name, newMethod.name, newMethod.desc))
							add(getRetForType(Type.getReturnType(firstMethod.desc)))
						}
					}
				}
			}
		}
	}
	
	fun incAllVarInsn(insnList: InsnList): InsnList {
		return insnList.apply {
			for (insn in this) {
				if (insn is VarInsnNode) {
					insn.`var` += 1
				}
			}
		}
	}
}
