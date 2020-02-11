package cookiedragon.obfuscator.runtime

import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 11/Feb/2020
 */
object OpaqueRuntimeManager: IClassProcessor {
	lateinit var classNode: ClassNode
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		classNode = ClassNode().apply {
			this.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
			this.version = classes.first().version
			this.name = ClassRenamer.namer.uniqueRandomString()
			this.signature = null
			this.superName = "java/lang/Object"
			classes.add(this)
			ClassPath.classes[this.name] = this
			ClassPath.classPath[this.name] = this
		}
	}
}
