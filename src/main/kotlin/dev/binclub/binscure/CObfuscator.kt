package dev.binclub.binscure

import dev.binclub.binscure.annotations.ExcludeAll
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.classpath.ClassPath.passThrough
import dev.binclub.binscure.classpath.ClassPathIO
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.configuration.exclusions.ExclusionConfiguration
import dev.binclub.binscure.configuration.exclusions.PackageBlacklistExcluder
import dev.binclub.binscure.processors.AbstractMethodImplementor
import dev.binclub.binscure.processors.arithmetic.ArithmeticSubstitutionTransformer
import dev.binclub.binscure.processors.classmerge.StaticMethodMerger
import dev.binclub.binscure.processors.constants.FieldInitialiser
import dev.binclub.binscure.processors.constants.NumberObfuscation
import dev.binclub.binscure.processors.constants.string.StringObfuscator
import dev.binclub.binscure.processors.debug.AccessStripper
import dev.binclub.binscure.processors.debug.KotlinMetadataStripper
import dev.binclub.binscure.processors.debug.SourceStripper
import dev.binclub.binscure.processors.exploit.BadAttributeExploit
import dev.binclub.binscure.processors.exploit.BadClinitExploit
import dev.binclub.binscure.processors.exploit.BadIndyConstant
import dev.binclub.binscure.processors.flow.CfgFucker
import dev.binclub.binscure.processors.flow.classinit.ClassInitMonitor
import dev.binclub.binscure.utils.internalName
import dev.binclub.binscure.utils.whenNotNull
import dev.binclub.binscure.processors.flow.trycatch.FakeTryCatch
import dev.binclub.binscure.processors.flow.trycatch.UselessTryCatch
import dev.binclub.binscure.processors.indirection.DynamicCallObfuscation
import dev.binclub.binscure.processors.optimisers.EnumValuesOptimiser
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.processors.renaming.impl.FieldRenamer
import dev.binclub.binscure.processors.renaming.impl.LocalVariableRenamer
import dev.binclub.binscure.processors.renaming.impl.MethodRenamer
import dev.binclub.binscure.processors.resources.ManifestResourceProcessor
import dev.binclub.binscure.processors.runtime.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.PrintWriter
import java.lang.reflect.Modifier
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

/**
 * @author cookiedragon234 20/Jan/2020
 */
object CObfuscator {
	val random = SecureRandom()
	val mappings = mutableMapOf<String, String>()
	
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
			//VariableInitializer,
			FieldInitialiser,
			AccessStripper,
			EnumValuesOptimiser,
			
			SourceStripper,
			KotlinMetadataStripper,
			
			LocalVariableRenamer,
			MethodRenamer,
			FieldRenamer,
			ClassRenamer,
			
			StringObfuscator,
			DynamicCallObfuscation,
			
			//JumpRearranger,
			ArithmeticSubstitutionTransformer,
			CfgFucker,
			BadClinitExploit,
			ClassInitMonitor,
			FakeTryCatch,
			UselessTryCatch,
			
			StaticMethodMerger,
			NumberObfuscation,
			
			BadAttributeExploit,
			BadIndyConstant,
			
			AbstractMethodImplementor,
			
			ManifestResourceProcessor
		)
		
		val classes = mutableListOf<ClassNode>()
		classes.addAll(ClassPath.classes.values)
		if (classes.isNotEmpty()) {
			var progress = 0f
			for (processor in processors) {
				try {
					debug(processor::class.java.simpleName)
					if (rootConfig.printProgress) {
						print(rootConfig.getLineChar())
						val percentStr = ((progress / processors.size) * 100).toInt().toString().padStart(3, ' ')
						print("$percentStr% - ${processor.progressDescription}".padEnd(100, ' '))
					}
					processor.process(classes, passThrough)
				} catch (t: Throwable) {
					println("\rException while processing [${processor.progressDescription}]:")
					t.printStackTrace()
				}
				progress += 1
			}
			if (rootConfig.printProgress) {
				print(rootConfig.getLineChar())
			}
		}
		ClassPath.classes[OpaqueRuntimeManager.classNode.name] = OpaqueRuntimeManager.classNode
		ClassPath.classPath[OpaqueRuntimeManager.classNode.name] = OpaqueRuntimeManager.classNode
		
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
