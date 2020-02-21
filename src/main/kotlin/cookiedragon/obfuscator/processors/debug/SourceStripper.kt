package cookiedragon.obfuscator.processors.debug

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.api.SourceStripConfiguration
import cookiedragon.obfuscator.configuration.ConfigurationManager.rootConfig
import cookiedragon.obfuscator.api.SourceStripConfiguration.LineNumberAction.*
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LineNumberNode

/**
 * @author cookiedragon234 22/Jan/2020
 */
object SourceStripper: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.sourceStrip.enabled)
			return
		
		val action = rootConfig.sourceStrip.lineNumbers
		
		for (classNode in CObfuscator.getProgressBar("Stripping Source Info").wrap(classes)) {
			classNode.sourceDebug = null
			classNode.sourceFile = null
			classNode.signature = null
			
			if (action == KEEP)
				continue
			
			for (method in classNode.methods) {
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn is LineNumberNode && action == REMOVE) {
						modifier.remove(insn)
					}
				}
				modifier.apply(method)
				
				method.exceptions = null
				method.signature = null
			}
		}
	}
}
