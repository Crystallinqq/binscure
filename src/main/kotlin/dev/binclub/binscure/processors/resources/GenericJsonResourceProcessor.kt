package dev.binclub.binscure.processors.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ContainerNode
import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.kotlin.wrap
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 27/Jan/2020
 */
object GenericJsonResourceProcessor: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		val mapper = ObjectMapper()
		
		
		for ((name, bytes) in CObfuscator.getProgressBar("Remapping Generic Jsons").wrap(passThrough)) {
			if (name.endsWith(".json") && name.contains("mixin")) {
				//ContainerNode
			//	val node = mapper.readValue<ContainerNode>(bytes, ContainerNode::class.java)
			}
		}
	}
}
