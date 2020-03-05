package cookiedragon.obfuscator.processors.renaming.impl

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.classpath.ClassTree
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.configuration.ConfigurationManager.rootConfig
import cookiedragon.obfuscator.kotlin.getOrPut
import cookiedragon.obfuscator.kotlin.getOrPutLazy
import cookiedragon.obfuscator.kotlin.toPrettyString
import cookiedragon.obfuscator.processors.renaming.AbstractRenamer
import cookiedragon.obfuscator.processors.renaming.generation.NameGenerator
import cookiedragon.obfuscator.processors.renaming.utils.CustomRemapper
import me.tongfei.progressbar.ProgressBar
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.lang.RuntimeException

/**
 * @author cookiedragon234 25/Jan/2020
 */
object MethodRenamer: AbstractRenamer() {
	override fun isEnabled(): Boolean = ConfigurationManager.rootConfig.remap.areMethodsEnabled()
	
	override fun getTaskName(): String = "Remapping Methods"
	
	override fun remap(
		progressBar: ProgressBar,
		remapper: CustomRemapper,
		classes: Collection<ClassNode>,
		passThrough: MutableMap<String, ByteArray>
	) {
		progressBar.extraMessage = "Methods"
		for (classNode in classes) {
			progressBar.step()
			if (CObfuscator.isExcluded(classNode))
				continue
			
			val classTree = ClassPath.hierachy[classNode.name] ?: continue
			val names = mutableMapOf<String, NameGenerator>()
			
			for (method in classNode.methods) {
				if (method.name == "main" && method.desc == "([Ljava/lang/String;)V")
					continue
				
				if (method.name.startsWith("<"))
					continue
				
				if (!parentsHaveMethod(classTree, method)) {
					val generator =  names.getOrPutLazy(method.desc) {NameGenerator(rootConfig.remap.methodPrefix)}
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
