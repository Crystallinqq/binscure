package dev.binclub.binscure.processors.renaming.impl

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.processors.renaming.AbstractRenamer
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 24/Jan/2020
 */
object ClassRenamer: AbstractRenamer() {
	override fun isEnabled(): Boolean = rootConfig.remap.areClassesEnabled()
	val namer = NameGenerator(rootConfig.remap.classPrefix)
	val keepPackages = false
	
	override fun remap(
		remapper: CustomRemapper,
		classes: Collection<ClassNode>,
		passThrough: MutableMap<String, ByteArray>
	) {
		for (classNode in classes) {
			//if (ignores.contains(classNode.name)) continue
			if (classNode.name.contains("entrypoint", true)) continue
			if (!CObfuscator.isExcluded(classNode)) {
				val name = if (keepPackages) {
					"${classNode.name.substringBeforeLast('/')}/${namer.uniqueRandomString()}"
				} else {
					namer.uniqueRandomString()
				}
				remapper.map(classNode.name, name)
			}
		}
	}
}
