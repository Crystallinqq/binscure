package dev.binclub.binscure.processors.debug

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.addAccess
import dev.binclub.binscure.utils.hasAccess
import dev.binclub.binscure.utils.removeAccess
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode

/**
 * This transformer removes all unecessary access flags, such as private, protected, final, etc
 *
 * @author cookiedragon234 21/Feb/2020
 */
object AccessStripper: IClassProcessor {
	override val progressDescription: String
		get() = "Stripping access flags"
	override val config: TransformerConfiguration
		get() = rootConfig
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (isExcluded(classNode))
				continue
			
			classNode.access = makePublic(classNode.access)
			
			for (method in classNode.methods) {
				// Dont run on static init
				if (method.name != "<clinit>" && !isExcluded(classNode, method)) {
					method.access = makePublic(method.access)
				}
			}
			
			for (field in classNode.fields) {
				if (isExcluded(classNode, field))
					continue
				
				field.access = makePublic(field.access, classNode.access.hasAccess(ACC_INTERFACE))
			}
		}
	}
	
	private fun makePublic(access: Int, isInterface: Boolean = false): Int {
		var access = access
		if (access.hasAccess(ACC_PRIVATE))
			access = access.removeAccess(ACC_PRIVATE)
		if (access.hasAccess(ACC_PROTECTED))
			access = access.removeAccess(ACC_PROTECTED)
		if (access.hasAccess(ACC_SYNTHETIC))
			access = access.removeAccess(ACC_SYNTHETIC)
		if (access.hasAccess(ACC_BRIDGE))
			access = access.removeAccess(ACC_BRIDGE)
		if (access.hasAccess(ACC_FINAL) && !isInterface)
			access = access.removeAccess(ACC_FINAL)
		if (!access.hasAccess(ACC_PUBLIC))
			access = access.addAccess(ACC_PUBLIC)
		return access
	}
}
