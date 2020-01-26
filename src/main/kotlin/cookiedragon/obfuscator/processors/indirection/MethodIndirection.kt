package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.kotlin.wrap
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @author cookiedragon234 22/Jan/2020
 */
object MethodIndirection: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.indirection.enabled)
			return
		
		for (classNode in CObfuscator.getProgressBar("Indirecting method calls").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				for (insn in method.instructions) {
					if (insn is MethodInsnNode) {
					
					}
				}
			}
		}
	}
}
