package dev.binclub.binscure.configuration.exclusions

import dev.binclub.binscure.utils.originalName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 22/Jan/2020
 */
class PackageWhitelistExcluder(val aPackage: String): ExclusionConfiguration() {
	override fun isExcluded(name: String) = !name.startsWith(aPackage)
	override fun isExcluded(classNode: ClassNode) = !(classNode.originalName?.startsWith(aPackage) ?: true)
	override fun isExcluded(parentClass: ClassNode, methodNode: MethodNode) = !(parentClass.originalName?.startsWith(aPackage) ?: true)
	override fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode) = !(parentClass.originalName?.startsWith(aPackage)?: true)
}
