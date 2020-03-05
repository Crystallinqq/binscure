package cookiedragon.obfuscator.processors.debug

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.addAccess
import cookiedragon.obfuscator.kotlin.hasAccess
import cookiedragon.obfuscator.kotlin.removeAccess
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import java.lang.reflect.Modifier

/**
 * @author cookiedragon234 21/Feb/2020
 */
object AccessStripper: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			classNode.access = makePublic(classNode.access)
			
			for (method in classNode.methods) {
				// Dont run on static init
				if (method.name != "<clinit>" && !CObfuscator.isExcluded(classNode, method)) {
					method.access = makePublic(method.access)
				}
			}
			
			for (field in classNode.fields) {
				if (CObfuscator.isExcluded(classNode, field))
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
		if (access.hasAccess(ACC_SYNTHETIC))
			access = access.removeAccess(ACC_SYNTHETIC)
		if (access.hasAccess(ACC_FINAL) && !isInterface)
			access = access.removeAccess(ACC_FINAL)
		if (!access.hasAccess(ACC_PUBLIC))
			access = access.addAccess(ACC_PUBLIC)
		return access
	}
}
