package dev.binclub.binscure.processors.renaming

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.utils.originalName
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import dev.binclub.binscure.utils.AnnotationFieldRemapper
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 24/Jan/2020
 */
abstract class AbstractRenamer: IClassProcessor {
	val ignores = arrayOf<String>(
		"io.github.giantnuker.pyro.PyroClassTransformer",
		"io.github.giantnuker.pyro.launcher.LauncherImpl",
		"io.github.giantnuker.pyro.d",
		"io.github.giantnuker.pyro.Pyro",
		"io.github.giantnuker.pyro.PyroLoaderConstants",
		"io.github.giantnuker.pyro.launcher.PyroMixinPlugin",
		"io.github.giantnuker.pyro.mixin.StopAsapMixin"
	)
	
	final override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!isEnabled())
			return
		
		val remapper = CustomRemapper()
		remap(remapper, classes, passThrough)
		val replacements = mutableMapOf<ClassNode, ClassNode>()
		for (classNode in classes) {
			val newNode = ClassNode()
			val classMapper = ClassRemapper(newNode, remapper)
			classNode.accept(classMapper)
			replacements[classNode] = newNode
			AnnotationFieldRemapper.remap(newNode, remapper)
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
		ClassPath.constructHierarchy()
	}
	
	protected abstract fun remap(remapper: CustomRemapper, classes: Collection<ClassNode>, passThrough: MutableMap<String, ByteArray>)
	protected abstract fun isEnabled(): Boolean
}
