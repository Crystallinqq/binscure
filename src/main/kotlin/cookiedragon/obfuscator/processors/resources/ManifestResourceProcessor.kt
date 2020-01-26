package cookiedragon.obfuscator.processors.resources

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
object ManifestResourceProcessor: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (entry in passThrough) {
			if (entry.key.endsWith(".json") || entry.key.endsWith(".MF")) {
				var contents = String(entry.value)
				for (mapping in CObfuscator.mappings) {
					if (!mapping.key.contains('.')) {
						contents = contents.replace(mapping.key.replace('/', '.'), mapping.value.replace('/', '.'))
					}
				}
				passThrough[entry.key] = contents.toByteArray()
			}
		}
	}
}
