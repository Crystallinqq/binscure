package dev.binclub.binscure.processors.resources

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.jar.Attributes
import java.util.jar.Manifest

/**
 * @author cookiedragon234 26/Jan/2020
 */
object ManifestResourceProcessor: IClassProcessor {
	override val progressDescription: String
		get() = "Processing manifests"
	override val config = rootConfig
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		val sorted = CObfuscator.mappings.entries.sortedByDescending { it.key.length }
		for ((name, bytes) in passThrough) {
			if (name.endsWith(".json", true)) {
				var contents = String(bytes)
				for (mapping in sorted) {
					if (!mapping.key.contains('.')) {
						contents = contents.replace(
							"\"${mapping.key.replace('/', '.')}\"",
							"\"${mapping.value.replace('/', '.')}\""
						)
					}
				}
				passThrough[name] = contents.toByteArray()
			} else if (name.endsWith(".MF", true)) {
				try {
					val manifest = Manifest(ByteArrayInputStream(bytes))
					fun processAttribute(attribute: Attributes) {
						for (entry in attribute.entries) {
							if (entry.value is String) {
								var contents = (entry.value as String).trim()
								for (mapping in sorted) {
									val key = mapping.key.replace('/', '.')
									if (contents == key) {
										contents = mapping.value.replace('/', '.')
									}
								}
								entry.setValue(contents)
							}
						}
					}
					processAttribute(manifest.mainAttributes)
					for ((name, attribute) in manifest.entries) {
						processAttribute(attribute)
					}
					passThrough[name] = ByteArrayOutputStream().also {
						manifest.write(it)
					}.toByteArray()
				} catch (t: Throwable) {
					t.printStackTrace()
				}
			}
		}
	}
}
