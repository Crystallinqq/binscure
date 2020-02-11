package cookiedragon.obfuscator

import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.classpath.ClassPath.passThrough
import cookiedragon.obfuscator.classpath.ClassPathIO
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.configuration.ConfigurationManager.rootConfig
import cookiedragon.obfuscator.configuration.exclusions.ExclusionConfiguration
import cookiedragon.obfuscator.configuration.exclusions.PackageBlacklistExcluder
import cookiedragon.obfuscator.processors.flow.classinit.ClassInitMonitor
import cookiedragon.obfuscator.processors.resources.ManifestResourceProcessor
import cookiedragon.obfuscator.processors.resources.MixinResourceProcessor
import me.tongfei.progressbar.CustomProcessRenderer
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import me.tongfei.progressbar.getConsoleConsumer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.PrintWriter
import java.lang.reflect.Modifier
import java.security.SecureRandom
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant

/**
 * @author cookiedragon234 20/Jan/2020
 */
object CObfuscator {
	val random = SecureRandom()
	var exclusions = arrayListOf<ExclusionConfiguration>().also {
		arr ->
		ConfigurationManager.rootConfig.exclusions.forEach{
			arr.add(PackageBlacklistExcluder(it))
		}
	}
	val mappings = mutableMapOf<String, String>()
	
	fun getProgressBar(taskName: String) =
		ProgressBar(
			" " + taskName.padEnd(24),
			0,
			200,
			CustomProcessRenderer(
				ProgressBarStyle.ASCII,
				"",
				1,
				false,
				DecimalFormat("#.0")
			),
			getConsoleConsumer()
		)
	
	operator fun invoke() {
		val start = Instant.now()
		if (!rootConfig.input.exists())
			throw FileNotFoundException("File ${rootConfig.input} does not exist")
		if (rootConfig.output.exists())
			System.err.println("Warning: Output file already exists, will be overwritten")
		if (rootConfig.output.exists() && !rootConfig.output.renameTo(rootConfig.output))
			System.err.println("Warning: Output file is currently in use by another process")
		
		ClassPathIO.loadInputJar(rootConfig.input)
		ClassPathIO.loadClassPath(rootConfig.libraries)
		
		ClassPath.constructHierarchy()
		
		val processors = arrayOf(
			//BadInvoke,
			//UselessTryCatch,
			//DynamicCallObfuscation
			//FakeTryCatch,
			//TableSwitchJump,
			ClassInitMonitor,
			/*
			OpaqueJumps,
			//NumberObfuscation,
			
			SourceStripper,
			KotlinMetadataStripper,
			
			LocalVariableRenamer,
			MethodRenamer,
			FieldRenamer,
			ClassRenamer,
			StringObfuscator,
			
			InvalidSignatureExploit,*/
			
			ManifestResourceProcessor,
			MixinResourceProcessor
		)
		
		val classes = mutableListOf<ClassNode>()
		classes.addAll(ClassPath.classes.values)
		if (classes.isNotEmpty()) {
			for (processor in processors) {
				processor.process(classes, passThrough)
			}
		}
		
		ClassPathIO.writeOutput(rootConfig.output)
		
		val duration = Duration.between(start, Instant.now())
		println("Finished processing ${classes.size} classes and ${passThrough.size} resources in ${duration.toMillis() / 1000f}s")
		
		if (rootConfig.mappingFile != null && !mappings.isEmpty()) {
			PrintWriter(FileOutputStream(rootConfig.mappingFile)).use {
				for ((key, value) in mappings) {
					it.println(key.replace(",", "\\,") + "," + value.replace(",", "\\,"))
				}
			}
		}
	}
	
	fun isExcluded(name: String): Boolean {
		for (exclusion in exclusions) {
			if (exclusion.isExcluded(name))
				return true
		}
		return false
	}
	fun isExcluded(classNode: ClassNode): Boolean {
		for (exclusion in exclusions) {
			if (exclusion.isExcluded(classNode))
				return true
		}
		return false
	}
	fun isExcluded(parentClass: ClassNode, methodNode: MethodNode): Boolean {
		for (exclusion in exclusions) {
			if (exclusion.isExcluded(parentClass, methodNode))
				return true
		}
		return false
	}
	fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode): Boolean {
		for (exclusion in exclusions) {
			if (exclusion.isExcluded(parentClass, fieldNode))
				return true
		}
		return false
	}
	
	fun noMethodInsns(methodNode: MethodNode) =
		Modifier.isAbstract(methodNode.access) || Modifier.isNative(methodNode.access)
	
	fun randomWeight(weight: Int): Boolean {
		return random.nextInt(weight) == 0
	}
}
