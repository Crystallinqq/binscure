package dev.binclub.binscure.processors.renaming.impl

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.processors.renaming.AbstractRenamer
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import me.tongfei.progressbar.ProgressBar
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 24/Jan/2020
 */
object ClassRenamer: AbstractRenamer() {
	override fun isEnabled(): Boolean = rootConfig.remap.areClassesEnabled()
	override fun getTaskName(): String = "Remapping Classes"
	val namer = NameGenerator(rootConfig.remap.classPrefix)
	
	override fun remap(
		progressBar: ProgressBar,
		remapper: CustomRemapper,
		classes: Collection<ClassNode>,
		passThrough: MutableMap<String, ByteArray>
	) {
		progressBar.extraMessage = "Classes"
		for (classNode in classes) {
			if (!CObfuscator.isExcluded(classNode)) {
				remapper.map(classNode.name, namer.uniqueRandomString())
			}
			progressBar.step()
		}
	}
}
