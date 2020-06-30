package dev.binclub.binscure.processors.resources

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 27/Jan/2020
 */
object GenericJsonResourceProcessor: IClassProcessor {
	override val progressDescription: String
		get() = "Processing json resources"
	override val config: TransformerConfiguration
		get() = rootConfig
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for ((name, bytes) in passThrough) {
			if (name.endsWith(".json") && name.contains("mixin")) {
				//ContainerNode
			//	val node = mapper.readValue<ContainerNode>(bytes, ContainerNode::class.java)
			}
		}
	}
}
