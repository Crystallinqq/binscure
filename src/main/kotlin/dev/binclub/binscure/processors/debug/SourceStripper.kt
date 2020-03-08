package dev.binclub.binscure.processors.debug

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.api.transformers.LineNumberAction.*
import dev.binclub.binscure.kotlin.wrap
import dev.binclub.binscure.utils.InstructionModifier
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
