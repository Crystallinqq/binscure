package dev.binclub.binscure.configuration.exclusions

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 22/Jan/2020
 */
abstract class ExclusionConfiguration {
	open fun isExcluded(name: String) = false
	open fun isExcluded(classNode: ClassNode) = false
	open fun isExcluded(parentClass: ClassNode, methodNode: MethodNode) = false
	open fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode) = false
}
