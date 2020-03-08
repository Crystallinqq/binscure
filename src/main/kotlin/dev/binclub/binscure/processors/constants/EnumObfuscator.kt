package dev.binclub.binscure.processors.constants

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.kotlin.hasAccess
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 07/Mar/2020
 */
object EnumObfuscator: IClassProcessor {
	val enumMappings: MutableMap<ClassNode, MutableMap<String, String>> = hashMapOf()
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		//for (classNode in classes) {
			//if (classNode.access.hasAccess(ACC_ENUM)) {
				//val mappings = enumMappings[classNode] ?: continue
				//
			//}
		//}
	}
}
