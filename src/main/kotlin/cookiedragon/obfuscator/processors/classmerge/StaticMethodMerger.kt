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
		val staticMethods = arrayListOf<Pair<ClassNode, MethodNode>>()
		
		for (classNode in classes) {
			for (method in classNode.methods) {
				if (
					method.access.hasAccess(ACC_STATIC)
					&&
					!method.access.hasAccess(ACC_ABSTRACT)
					&&
					!method.access.hasAccess(ACC_NATIVE)
				) {
					println("Did ${classNode.name}.${method.name}")
					staticMethods.add(Pair(classNode, method))
				} else {
					println("Skipped ${classNode.name}.${method.name}")
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
			
			for ((classNode, method) in staticMethods) {
				println("Processed ${classNode.name}.${method.name}")
				
				var newNode: ClassNode
				do {
					newNode = classes.random(random)
				} while (newNode.access.hasAccess(ACC_INTERFACE))
				
				val newMethod = MethodNode(
					ACC_PUBLIC + ACC_STATIC,
					namer.uniqueRandomString(),
					method.desc,
					null,
					null)
				newNode.methods.add(newMethod)
				
				newMethod.tryCatchBlocks = method.tryCatchBlocks
				method.tryCatchBlocks = null
				
				newMethod.localVariables = method.localVariables
				method.localVariables = null
				
				newMethod.instructions = method.instructions.clone()
				method.instructions = InsnList().apply {
					val params = Type.getArgumentTypes(method.desc)
					for ((index, param) in params.withIndex()) {
						add(VarInsnNode(getLoadForType(param), index))
					}
					add(MethodInsnNode(INVOKESTATIC, newNode.name, newMethod.name, newMethod.desc))
					add(getRetForType(Type.getReturnType(method.desc)))
				}
				println("Finished ${classNode.name}.${method.name}")
			}
		}
	}
}
