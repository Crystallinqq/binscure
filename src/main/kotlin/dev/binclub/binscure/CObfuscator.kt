package dev.binclub.binscure

import dev.binclub.binscure.annotations.ExcludeAll
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.classpath.ClassPath.passThrough
import dev.binclub.binscure.classpath.ClassPathIO
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.configuration.exclusions.ExclusionConfiguration
import dev.binclub.binscure.configuration.exclusions.PackageBlacklistExcluder
import dev.binclub.binscure.kotlin.internalName
import dev.binclub.binscure.kotlin.whenNotNull
import dev.binclub.binscure.processors.classmerge.StaticMethodMerger
import dev.binclub.binscure.processors.constants.FieldInitialiser
import dev.binclub.binscure.processors.constants.NumberObfuscation
import dev.binclub.binscure.processors.constants.StringObfuscator
import dev.binclub.binscure.processors.debug.AccessStripper
import dev.binclub.binscure.processors.debug.KotlinMetadataStripper
import dev.binclub.binscure.processors.debug.SourceStripper
import dev.binclub.binscure.processors.exploit.BadAttributeExploit
import dev.binclub.binscure.processors.exploit.BadClinit
import dev.binclub.binscure.processors.exploit.BadIndyConstant
import dev.binclub.binscure.processors.flow.CfgFucker
import dev.binclub.binscure.processors.flow.classinit.ClassInitMonitor
import dev.binclub.binscure.processors.flow.trycatch.FakeTryCatch
import dev.binclub.binscure.processors.flow.trycatch.UselessTryCatch
import dev.binclub.binscure.processors.indirection.DynamicCallObfuscation
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.processors.renaming.impl.FieldRenamer
import dev.binclub.binscure.processors.renaming.impl.LocalVariableRenamer
import dev.binclub.binscure.processors.renaming.impl.MethodRenamer
import dev.binclub.binscure.processors.resources.ManifestResourceProcessor
import dev.binclub.binscure.runtime.OpaqueRuntimeManager
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
	var exclusions = arrayListOf<ExclusionConfiguration>().also { arr ->
		rootConfig.exclusions.forEach{
			arr.add(PackageBlacklistExcluder(it.trim()))
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
			OpaqueRuntimeManager,
			FieldInitialiser,
			AccessStripper,
			
			SourceStripper,
			KotlinMetadataStripper,
			
			LocalVariableRenamer,
			MethodRenamer,
			FieldRenamer,
			ClassRenamer,
			
			StringObfuscator,
			//DynamicCallObfuscation,
			
			CfgFucker,
			BadClinit,
			ClassInitMonitor,
			FakeTryCatch,
			UselessTryCatch,
			
			StaticMethodMerger,
			NumberObfuscation,
			
			BadAttributeExploit,
			BadIndyConstant,
			
			ManifestResourceProcessor
		)
		
		val classes = mutableListOf<ClassNode>()
		classes.addAll(ClassPath.classes.values)
		if (classes.isNotEmpty()) {
			var progress = 0
			for (processor in processors) {
				try {
					processor.process(classes, passThrough)
					debug(processor::class.java.simpleName)
					print("\r${(progress / (processors.size - 1)) * 100}%")
				} catch (t: Throwable) {
					println("Exception while processing ${processor::class.java.simpleName}")
					t.printStackTrace()
				}
				progress += 1
			}
			print("\r")
		}
		OpaqueRuntimeManager.classNode.apply {
			ClassPath.classes[this.name] = this
			ClassPath.classPath[this.name] = this
		}
		
		ClassPathIO.writeOutput(rootConfig.output)
		
		val duration = Duration.between(start, Instant.now())
		println("Finished processing ${classes.size} classes and ${passThrough.size} resources in ${duration.toMillis() / 1000f}s")
		
		rootConfig.mappingFile.whenNotNull {file ->
			if (mappings.isNotEmpty()) {
				PrintWriter(FileOutputStream(file)).use {
					for ((key, value) in mappings) {
						it.println(key.replace(",", "\\,") + "," + value.replace(",", "\\,"))
					}
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
		if (classNode == OpaqueRuntimeManager.classNode) return true
		if (classNode.visibleAnnotations?.any { it.desc == ExcludeAll::class.internalName } == true) return true
		for (exclusion in exclusions) {
			if (exclusion.isExcluded(classNode))
				return true
		}
		return false
	}
	fun isExcluded(parentClass: ClassNode, methodNode: MethodNode): Boolean {
		if (methodNode.visibleAnnotations?.any { it.desc == ExcludeAll::class.internalName } == true) return true
		for (exclusion in exclusions) {
			if (exclusion.isExcluded(parentClass, methodNode))
				return true
		}
		return false
	}
	fun isExcluded(parentClass: ClassNode, fieldNode: FieldNode): Boolean {
		if (fieldNode.visibleAnnotations?.any { it.desc == ExcludeAll::class.internalName } == true) return true
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
	
	inline fun debug(message: Any) {
		if (false) {
			println(message)
		}
	}
}
