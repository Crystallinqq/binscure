package dev.binclub.binscure.processors.renaming.impl

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.processors.renaming.AbstractRenamer
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 24/Jan/2020
 */
object ClassRenamer: AbstractRenamer() {
	override fun isEnabled(): Boolean = config.areClassesEnabled()
	override val progressDescription: String
		get() = "Renaming classes"
	val namer = NameGenerator(config.classPrefix)
	val keepPackages = false
	
	override fun remap(
		remapper: CustomRemapper,
		classes: Collection<ClassNode>,
		passThrough: MutableMap<String, ByteArray>
	) {
		forClass(classes) { classNode ->
			val name = if (keepPackages) {
				"${classNode.name.substringBeforeLast('/')}/${namer.uniqueRandomString()}"
			} else {
				namer.uniqueRandomString()
			}
			remapper.map(classNode.name, name)
		}
	}
}
