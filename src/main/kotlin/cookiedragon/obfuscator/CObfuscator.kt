package cookiedragon.obfuscator

import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.classpath.ClassPath.classes
import cookiedragon.obfuscator.classpath.ClassPath.passThrough
import cookiedragon.obfuscator.classpath.ClassPathIO
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.configuration.ConfigurationManager.rootConfig
import cookiedragon.obfuscator.configuration.exclusions.ExclusionConfiguration
import cookiedragon.obfuscator.configuration.exclusions.PackageBlacklistExcluder
import cookiedragon.obfuscator.configuration.exclusions.PackageWhitelistExcluder
import cookiedragon.obfuscator.processors.debug.KotlinMetadataStripper
import cookiedragon.obfuscator.processors.debug.SourceStripper
import cookiedragon.obfuscator.processors.exploit.InvalidSignatureExploit
import cookiedragon.obfuscator.processors.indirection.MethodIndirection
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.processors.renaming.impl.FieldRenamer
import cookiedragon.obfuscator.processors.renaming.impl.LocalVariableRenamer
import cookiedragon.obfuscator.processors.renaming.impl.MethodRenamer
import cookiedragon.obfuscator.processors.resources.ManifestResourceProcessor
import cookiedragon.obfuscator.processors.string.StringObfuscator
import me.tongfei.progressbar.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.PrintWriter
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
			SourceStripper,
			KotlinMetadataStripper,
			
			MethodIndirection,
			
			LocalVariableRenamer,
			MethodRenamer,
			FieldRenamer,
			ClassRenamer,
			
			StringObfuscator,
			
			InvalidSignatureExploit,
			
			
			ManifestResourceProcessor
		)
		
		val classes = mutableListOf<ClassNode>()
		classes.addAll(ClassPath.classes.values)
		for (processor in processors) {
			processor.process(classes, passThrough)
		}
		
		ClassPathIO.writeOutput(rootConfig.output)
		
		val duration = Duration.between(start, Instant.now())
		println("Finished processing ${classes.size} classes and ${passThrough.size} resources in ${duration.toMillis() / 1000f}s")
		
		if (rootConfig.mappingFile != null && !mappings.isEmpty()) {
			PrintWriter(FileWriter(rootConfig.mappingFile)).use {
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
}
