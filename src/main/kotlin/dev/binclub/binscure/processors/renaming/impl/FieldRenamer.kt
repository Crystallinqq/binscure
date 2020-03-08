package dev.binclub.binscure.processors.renaming.impl

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.classpath.ClassTree
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.kotlin.hasAccess
import dev.binclub.binscure.processors.constants.EnumObfuscator
import dev.binclub.binscure.processors.renaming.AbstractRenamer
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import me.tongfei.progressbar.ProgressBar
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.tree.ClassNode
import java.lang.RuntimeException

/**
 * @author cookiedragon234 25/Jan/2020
 */
object FieldRenamer: AbstractRenamer() {
	override fun isEnabled(): Boolean = ConfigurationManager.rootConfig.remap.areFieldsEnabled()
	
	override fun getTaskName(): String = "Remapping Fields"
	
	override fun remap(
		progressBar: ProgressBar,
		remapper: CustomRemapper,
		classes: Collection<ClassNode>,
		passThrough: MutableMap<String, ByteArray>
	) {
		for (classNode in classes) {
			if (!CObfuscator.isExcluded(classNode)) {
				val names = mutableMapOf<String, NameGenerator>()
				val classTree = ClassPath.hierachy[classNode.name] ?: throw RuntimeException("$classNode not in classpath")
				
				for (field in classNode.fields) {
					
					val generator =  names.getOrPut(field.desc) {NameGenerator(rootConfig.remap.fieldPrefix)}
					var newName: String
					do {
						newName = generator.uniqueRandomString()
					} while (
						remapper.newFieldMappingExists(classNode.name, newName, field.desc)
						||
						!checkConflictingDownwardsRemaps(remapper, classTree, newName, field.desc)
					)
					
					if (field.access.hasAccess(ACC_ENUM) && classNode.access.hasAccess(ACC_ENUM)) {
						EnumObfuscator.enumMappings.getOrPut(classNode, { hashMapOf() })[newName] = field.name
					}
					
					if (!remapper.mapFieldName(classNode.name, field.name, field.desc, newName, false))
						throw IllegalStateException("Illegal State mapping methods (Race Condition?)")
					remapChildren(remapper, classTree, field.name, field.desc, newName)
				}
			}
			
			progressBar.step()
		}
	}
	
	private fun checkConflictingDownwardsRemaps(remapper: CustomRemapper, classTree: ClassTree, newName: String, desc: String): Boolean {
		for (child in classTree.children) {
			if (remapper.newFieldMappingExists(child.getName(), newName, desc))
				return false
			
			val childTree = ClassPath.hierachy[child.getName()] ?: continue
			if (!checkConflictingDownwardsRemaps(remapper, childTree, newName, desc))
				return false
		}
		return true
	}
	
	private fun remapChildren(remapper: CustomRemapper, classTree: ClassTree, name: String, desc: String, newName: String) {
		for (child in classTree.children) {
			remapper.mapFieldName(child.getName(), name, desc, newName, true)
			
			val childTree = ClassPath.hierachy[child.getName()] ?: continue
			remapChildren(remapper, childTree, name, desc, newName)
		}
	}
}
