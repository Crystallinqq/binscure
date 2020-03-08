package cookiedragon.obfuscator.processors.constants

import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.hasAccess
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
