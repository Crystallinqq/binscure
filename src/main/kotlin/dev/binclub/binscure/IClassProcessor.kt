package dev.binclub.binscure

import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.isExcluded
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.security.SecureRandom

/**
 * @author cookiedragon234 20/Jan/2020
 */
interface IClassProcessor {
	val random: SecureRandom
		get() = CObfuscator.random
	
	val progressDescription: String
	val config: TransformerConfiguration
	fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>)
	
	fun isExcluded(classNode: String): Boolean {
		return if (config != rootConfig) {
			rootConfig.exclusions.isExcluded(classNode) || config.exclusions.isExcluded(classNode)
		} else {
			config.exclusions.isExcluded(classNode)
		}
	}
	fun isExcluded(classNode: ClassNode): Boolean {
		return if (config != rootConfig) {
			rootConfig.exclusions.isExcluded(classNode) || config.exclusions.isExcluded(classNode)
		} else {
			config.exclusions.isExcluded(classNode)
		}
	}
	fun isExcluded(classNode: ClassNode, methodNode: MethodNode): Boolean {
		return if (config != rootConfig) {
			rootConfig.exclusions.isExcluded(classNode, methodNode) || config.exclusions.isExcluded(classNode, methodNode)
		} else {
			config.exclusions.isExcluded(classNode, methodNode)
		}
	}
	fun isExcluded(classNode: ClassNode, fieldNode: FieldNode): Boolean {
		return if (config != rootConfig) {
			rootConfig.exclusions.isExcluded(classNode, fieldNode) || config.exclusions.isExcluded(classNode, fieldNode)
		} else {
			config.exclusions.isExcluded(classNode, fieldNode)
		}
	}
}
