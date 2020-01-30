package cookiedragon.obfuscator.processors.constants

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.isNumberLdc
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 30/Jan/2020
 */
object NumberObfuscation: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Obfuscating Numbers").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				for (insn in method.instructions) {
					if (isNumberLdc(insn)) {
						println("Found num ${classNode.name}.${method.name}")
					}
				}
			}
		}
	}
}
