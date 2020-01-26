package cookiedragon.obfuscator.processors.resources

import cookiedragon.obfuscator.IClassProcessor
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 26/Jan/2020
 */
object MixinResourceProcessor: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for ((name, bytes) in passThrough) {
			if (name.endsWith(".json") && name.contains("mixin")) {
				val asString = String(bytes)
				
			}
		}
	}
}
