package cookiedragon.obfuscator

import org.objectweb.asm.tree.ClassNode
import java.security.SecureRandom

/**
 * @author cookiedragon234 20/Jan/2020
 */
interface IClassProcessor {
	fun getRandom(): SecureRandom = CObfuscator.random
	
	fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>)
}
