package dev.binclub.binscure.processors.resources
/*
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import org.objectweb.asm.tree.ClassNode
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * @author cookiedragon234 26/Jan/2020
 */
object MixinResourceProcessor: IClassProcessor {
	override val progressDescription: String
		get() = "Processing mixin resources"
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		val mapper = ObjectMapper()
		
		for ((name, bytes) in passThrough) {
			if (name.endsWith(".json") && name.contains("mixin")) {
				val objectNode = mapper.readValue<ObjectNode>(bytes, ObjectNode::class.java)
				
				val originalPackage = objectNode.get("package")?.textValue()?.replace('.', '/')
				
				var `package` = rootConfig.remap.classPrefix
				if (!`package`.endsWith('/')) {
					`package` = `package`.substring(0, `package`.lastIndexOf('/'))
				}
				if (originalPackage != null) {
					objectNode.put("package", `package`)
				}
				
				if (objectNode.get("mixins") != null) {
					val mixins = objectNode.get("mixins") as ArrayNode
					for (mapping in CObfuscator.mappings) {
						if (!mapping.key.contains('.') && mapping.key.startsWith(originalPackage!!)) {
							val mappingKey = mapping.key.substring(originalPackage.length + 1).replace('/', '.')
							val mappingVal = mapping.value.substring(`package`.length).replace('/', '.')
							
							for ((index, mixin) in mixins.withIndex()) {
								if (mixin.asText() == mappingKey) {
									mixins[index] = mixins.textNode(mappingVal)
								}
							}
						}
					}
					objectNode.replace("mixins", mixins)
				}
				
				passThrough[name] = mapper.writeValueAsBytes(objectNode)
			}
		}
	}
}
*/
