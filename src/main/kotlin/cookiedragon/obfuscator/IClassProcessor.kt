package cookiedragon.obfuscator

import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 20/Jan/2020
 */
interface IClassProcessor {
	fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>)
}
