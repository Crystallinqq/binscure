package dev.binclub.binscure.processors.debug

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.api.transformers.LineNumberAction.*
import dev.binclub.binscure.utils.InstructionModifier
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LineNumberNode

/**
 * This transformer removes unecessary debugging information typically emitted by javac from class file
 *
 * @author cookiedragon234 22/Jan/2020
 */
object SourceStripper: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.sourceStrip.enabled)
			return
		
		val action = rootConfig.sourceStrip.lineNumbers
		
		for (classNode in classes) {
			classNode.sourceDebug = null
			classNode.sourceFile = null
			classNode.signature = null
			classNode.innerClasses?.clear()
			
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
