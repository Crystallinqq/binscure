package dev.binclub.binscure.configuration.exclusions

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
class PackageBlacklistExcluder(inPackage: String): ExclusionConfiguration() {
	private val onlyClasses: Boolean = inPackage.endsWith(';')
	private val aPackage: String = inPackage.removeSuffix(";")
	
	override fun isExcluded(className: String) = className.startsWith(aPackage)
	override fun isExcluded(classNode: ClassNode) = isExcluded(classNode.originalName ?: classNode.name)
	override fun isExcluded(parentClass: ClassNode, methodNode: MethodNode) =
		!onlyClasses && isExcluded("${parentClass.originalName ?: parentClass.name}.${methodNode.name}")
	override fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode) =
		!onlyClasses && isExcluded("${parentClass.originalName ?: parentClass.name}.${fieldNode.name}")
	
	override fun toString(): String {
		return aPackage
	}
}
