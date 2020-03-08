package dev.binclub.binscure.processors.resources

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.kotlin.wrap
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
object ManifestResourceProcessor: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for ((name, bytes) in passThrough) {
			if (name.endsWith(".json") || name.endsWith(".MF")) {
				var contents = String(bytes)
				for (mapping in CObfuscator.mappings) {
					if (!mapping.key.contains('.')) {
						contents = contents.replace(mapping.key.replace('/', '.'), mapping.value.replace('/', '.'))
					}
				}
				passThrough[name] = contents.toByteArray()
			}
		}
	}
}
