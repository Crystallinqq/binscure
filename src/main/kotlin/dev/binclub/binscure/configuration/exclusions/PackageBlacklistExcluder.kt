package dev.binclub.binscure.configuration.exclusions

import dev.binclub.binscure.utils.originalName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
class PackageBlacklistExcluder(val aPackage: String): ExclusionConfiguration() {
	override fun isExcluded(name: String) = name.startsWith(aPackage)
	override fun isExcluded(classNode: ClassNode) = (classNode.originalName?.startsWith(aPackage) ?: false)
	override fun isExcluded(parentClass: ClassNode, methodNode: MethodNode) = (parentClass.originalName?.startsWith(aPackage) ?: false)
	override fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode) = (parentClass.originalName?.startsWith(aPackage)?: false)
}
