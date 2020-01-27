package cookiedragon.obfuscator.processors.resources

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
object ManifestResourceProcessor: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for ((name, bytes) in CObfuscator.getProgressBar("Remapping Manifest").wrap(passThrough)) {
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
