package cookiedragon.obfuscator.processors.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ContainerNode
import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
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
				val node = mapper.readValue<ContainerNode>(bytes, ContainerNode::class.java)
			}
		}
	}
}
