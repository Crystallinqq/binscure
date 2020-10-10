package dev.binclub.binscure.configuration.exclusions

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
class PackageBlacklistExcluder(inPackage: String): ExclusionConfiguration() {
	private val start: Boolean = inPackage.endsWith('-')
	private val stop: Boolean = inPackage.endsWith(';')
	private val aPackage: String = inPackage.removeSuffix(";").removeSuffix("-")
	
	override fun isExcluded(className: String) = when {
		start -> className.length > aPackage.length && className.startsWith(aPackage)
		stop -> className == aPackage
		else -> className.startsWith(aPackage)
	}
	override fun isExcluded(classNode: ClassNode) = isExcluded(classNode.originalName ?: classNode.name)
	override fun isExcluded(parentClass: ClassNode, methodNode: MethodNode) =
		!isExcluded("${parentClass.originalName ?: parentClass.name}.${methodNode.name}")
	override fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode) =
		isExcluded("${parentClass.originalName ?: parentClass.name}.${fieldNode.name}:${fieldNode.desc}")
	
	override fun toString(): String {
		return aPackage
	}
}
