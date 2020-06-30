package dev.binclub.binscure.utils

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.configuration.exclusions.ExclusionConfiguration
import dev.binclub.binscure.processors.runtime.OpaqueRuntimeManager
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 30/Jun/2020
 */
fun Collection<ExclusionConfiguration>.isExcluded(className: String) = this.any { it.isExcluded(className) }
fun Collection<ExclusionConfiguration>.isExcluded(classNode: ClassNode) = (classNode == OpaqueRuntimeManager.getClassNodeSafe()) || isExcluded(classNode.originalName ?: classNode.name)
fun Collection<ExclusionConfiguration>.isExcluded(parentClass: ClassNode, methodNode: MethodNode) = isExcluded("${parentClass.originalName ?: parentClass.name}.${methodNode.name}")
fun Collection<ExclusionConfiguration>.isExcluded(parentClass: ClassNode, fieldNode: FieldNode) = isExcluded("${parentClass.originalName ?: parentClass.name}.${fieldNode.name}")
