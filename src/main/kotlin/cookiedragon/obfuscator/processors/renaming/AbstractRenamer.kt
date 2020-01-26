package cookiedragon.obfuscator.processors.renaming

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.kotlin.originalName
import cookiedragon.obfuscator.processors.renaming.utils.CustomRemapper
import me.tongfei.progressbar.ProgressBar
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 24/Jan/2020
 */
abstract class AbstractRenamer: IClassProcessor {
	final override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!isEnabled())
			return
		
		val progressBar = CObfuscator.getProgressBar(getTaskName())
		progressBar.maxHint((classes.size * 2).toLong())
		
		val remapper = CustomRemapper()
		remap(progressBar, remapper, classes, passThrough)
		val replacements = mutableMapOf<ClassNode, ClassNode>()
		for (classNode in classes) {
			val newNode = ClassNode()
			val classMapper = ClassRemapper(newNode, remapper)
			classNode.accept(classMapper)
			replacements[classNode] = newNode
			
			progressBar.step()
		}
		
		for ((old, new) in replacements) {
			classes.remove(old)
			classes.add(new)
			ClassPath.classes.remove(old.name)
			ClassPath.classes[new.name] = new
			ClassPath.classPath.remove(old.name, old)
			ClassPath.classPath[new.name] = new
			ClassPath.originalNames[new] = old.originalName ?: continue
		}
		
		CObfuscator.mappings.putAll(remapper.dumpMappings())
		progressBar.close()
		ClassPath.constructHierarchy()
	}
	
	abstract fun remap(progressBar: ProgressBar, remapper: CustomRemapper, classes: Collection<ClassNode>, passThrough: MutableMap<String, ByteArray>)
	
	abstract fun getTaskName(): String
	abstract fun isEnabled(): Boolean
}
