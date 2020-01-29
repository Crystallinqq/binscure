package cookiedragon.obfuscator.processors.renaming.impl

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.configuration.ConfigurationManager.rootConfig
import cookiedragon.obfuscator.processors.renaming.AbstractRenamer
import cookiedragon.obfuscator.processors.renaming.generation.NameGenerator
import cookiedragon.obfuscator.processors.renaming.utils.CustomRemapper
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
