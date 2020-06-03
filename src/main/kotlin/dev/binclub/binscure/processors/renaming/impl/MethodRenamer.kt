package dev.binclub.binscure.processors.renaming.impl

import dev.binclub.binscure.CObfuscator.isExcluded
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.classpath.ClassTree
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.getOrPutLazy
import dev.binclub.binscure.utils.hasAccess
import dev.binclub.binscure.processors.renaming.AbstractRenamer
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 25/Jan/2020
 */
object MethodRenamer: AbstractRenamer() {
	override fun isEnabled(): Boolean = rootConfig.remap.areMethodsEnabled()
	override val progressDescription: String
		get() = "Renaming methods"
	
	override fun remap(
		remapper: CustomRemapper,
		classes: Collection<ClassNode>,
		passThrough: MutableMap<String, ByteArray>
	) {
		for (classNode in classes) {
			//if (ignores.contains(classNode.name)) continue
			if (isExcluded(classNode))
				continue
			
			val classTree = ClassPath.hierachy[classNode.name] ?: continue
			val names = mutableMapOf<String, NameGenerator>()
			
			for (method in classNode.methods) {
				//if (classNode.name.contains("entrypoint", true) && method.name.contains("start", true)) continue
				if (method.name == "main" && method.desc == "([Ljava/lang/String;)V")
					continue
				
				if (method.name.startsWith("<"))
					continue
				
				if (classNode.access.hasAccess(ACC_ENUM) && method.name == "values")
					continue
				
				if (!parentsHaveMethod(classTree, method)) {
					val desc = if (rootConfig.remap.aggressiveOverloading) method.desc else method.desc.substringBefore(")")
					val generator =  names.getOrPutLazy(desc) {NameGenerator(rootConfig.remap.methodPrefix)}
					var newName: String
					do {
						newName = generator.uniqueRandomString()
					} while (
						remapper.newMethodMappingExists(classNode.name, newName, method.desc)
						||
						!checkConflictingDownwardsRemaps(remapper, classTree, newName, method.desc)
					)
					if (!remapper.mapMethodName(classNode.name, method.name, method.desc, newName, false))
						throw IllegalStateException("Illegal State mapping methods (Race Condition?)")
					remapChildren(remapper, classTree, method.name, method.desc, newName)
				}
			}
		}
	}
	
	private fun checkConflictingDownwardsRemaps(remapper: CustomRemapper, classTree: ClassTree, newName: String, desc: String): Boolean {
		for (child in classTree.children) {
			if (remapper.newMethodMappingExists(child.getName(), newName, desc))
				return false
			
			val childTree = ClassPath.hierachy[child.getName()] ?: continue
			if (!checkConflictingDownwardsRemaps(remapper, childTree, newName, desc))
				return false
		}
		return true
	}
	
	private fun remapChildren(remapper: CustomRemapper, classTree: ClassTree, name: String, desc: String, newName: String) {
		for (child in classTree.children) {
			remapper.mapMethodName(child.getName(), name, desc, newName, true)
			
			val childTree = ClassPath.hierachy[child.getName()] ?: continue
			remapChildren(remapper, childTree, name, desc, newName)
		}
	}
	
	private fun parentsHaveMethod(classTree: ClassTree, methodNode: MethodNode): Boolean {
		for (parent in classTree.parents) {
			val parentTree = ClassPath.hierachy[parent.getName()] ?: continue
			if (hasMethod(parentTree, methodNode))
				return true
		}
		return false
	}
	
	private fun hasMethod(classTree: ClassTree, methodNode: MethodNode): Boolean {
		for (method in classTree.thisClass.getMethods()) {
			if (method.name == methodNode.name && method.desc == methodNode.desc) {
				return true
			}
		}
		for (parent in classTree.parents) {
			val parentTree = ClassPath.hierachy[parent.getName()] ?: continue
			if (hasMethod(parentTree, methodNode)) {
				return true
			}
		}
		return false
	}
}
