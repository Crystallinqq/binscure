package dev.binclub.binscure

import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.classpath.ClassPath.passThrough
import dev.binclub.binscure.classpath.ClassPathIO
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.processors.arithmetic.ArithmeticSubstitutionTransformer
import dev.binclub.binscure.processors.arithmetic.MbaTransformer
import dev.binclub.binscure.processors.classmerge.StaticMethodMerger
import dev.binclub.binscure.processors.constants.FieldInitialiser
import dev.binclub.binscure.processors.constants.NumberObfuscation
import dev.binclub.binscure.processors.constants.string.StringObfuscator
import dev.binclub.binscure.processors.debug.AccessStripper
import dev.binclub.binscure.processors.debug.KotlinMetadataStripper
import dev.binclub.binscure.processors.debug.SourceStripper
import dev.binclub.binscure.processors.exploit.BadAttributeExploit
import dev.binclub.binscure.processors.exploit.BadIndyConstant
import dev.binclub.binscure.processors.exploit.BadRecafAttributeExploit
import dev.binclub.binscure.processors.flow.CfgFucker
import dev.binclub.binscure.processors.flow.MethodParameterObfuscator
import dev.binclub.binscure.processors.flow.classinit.ClassInitMonitor
import dev.binclub.binscure.processors.flow.loop.LoopUnroller
import dev.binclub.binscure.utils.whenNotNull
import dev.binclub.binscure.processors.flow.trycatch.FakeTryCatch
import dev.binclub.binscure.processors.flow.trycatch.TryCatchDuplication
import dev.binclub.binscure.processors.flow.trycatch.UselessTryCatch
import dev.binclub.binscure.processors.indirection.DynamicCallObfuscation
import dev.binclub.binscure.processors.optimisers.EnumValuesOptimiser
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.processors.renaming.impl.FieldRenamer
import dev.binclub.binscure.processors.renaming.impl.LocalVariableRenamer
import dev.binclub.binscure.processors.renaming.impl.MethodRenamer
import dev.binclub.binscure.processors.resources.ManifestResourceProcessor
import dev.binclub.binscure.processors.runtime.*
import dev.binclub.binscure.utils.disableIllegalAccessWarning
import org.objectweb.asm.tree.ClassNode
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
	lateinit var hashParts: IntArray
	
	operator fun invoke() {
		disableIllegalAccessWarning()
		
		val license = Licenser.license
		if (license == null) {
			println("Invalid License File. Please email x4e_x4e@protonmail.com for assistance.")
			return
		} else {
			println("Found valid username and password at ${Licenser.licenseFile}")
		}
		hashParts = license.hashParts
		
		/*val parts = license.hashParts
		fun assertEq(a: Int, b: Int) {
			if (a != b) {
				error("Mismatch $a vs $b")
			}
		}
		assertEq(parts[0], 0x9122)
		assertEq(parts[1], 0x423)
		assertEq(parts[2], 0x9219)
		assertEq(parts[3], 0)
		assertEq(parts[4], 0x721AB)
		assertEq(parts[5], 0xFFFF)
		assertEq(parts[6], -0xFFF)
		assertEq(parts[7], 0x912ED)
		assertEq(parts[8], 0x91BCD)
		assertEq(parts[9], -0x128D)*/
		
		val start = Instant.now()
		if (!rootConfig.input.exists())
			throw FileNotFoundException("File ${rootConfig.input} does not exist")
		if (rootConfig.output.exists())
			System.err.println("Warning: Output file already exists, will be overwritten")
		if (rootConfig.output.exists() && !rootConfig.output.renameTo(rootConfig.output))
			System.err.println("Warning: Output file is currently in use by another process")
		
		ClassPathIO.loadInputJar(rootConfig.input)
		ClassPathIO.loadClassPath(rootConfig.libraries)
		
		ClassPath.reconstructHierarchy()
		
		val processors = arrayOf(
			FieldInitialiser,
			AccessStripper,
			EnumValuesOptimiser,
			
			SourceStripper,
			KotlinMetadataStripper,
			
			MethodParameterObfuscator,
			
			LocalVariableRenamer,
			MethodRenamer,
			FieldRenamer,
			ClassRenamer,
			
			StringObfuscator,
			DynamicCallObfuscation,
			
			ArithmeticSubstitutionTransformer,
			CfgFucker,
			ClassInitMonitor,
			FakeTryCatch,
			UselessTryCatch,
			TryCatchDuplication,
			
			StaticMethodMerger,
			NumberObfuscation,
			
			BadAttributeExploit,
			BadRecafAttributeExploit,
			BadIndyConstant,
			MbaTransformer,
			
			//LoopUnroller,
			//AbstractMethodImplementor, this is dumb

			ManifestResourceProcessor
		)
		
		val classes = mutableListOf<ClassNode>()
		classes.addAll(ClassPath.classes.values)
		if (classes.isNotEmpty()) {
			var progress = 0f
			for (processor in processors) {
				try {
					debug(processor::class.java.simpleName)
					if (rootConfig.printProgress && processor.config.enabled) {
						print(rootConfig.getLineChar())
						val percentStr = ((progress / processors.size) * 100).toInt().toString().padStart(3, ' ')
						print("$percentStr% - ${processor.progressDescription}".padEnd(100, ' '))
					}
					for (i in 0 until (hashParts[0] - 0x9121)) { // for i in 0 until 1
						processor.process(classes, passThrough)
					}
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
		
		for (i in 0..(hashParts[8] - 0x91BCD)) { // for i in 0..0
			val duration = Duration.between(start, Instant.now())
			println("Finished processing ${classes.size} classes and ${passThrough.size} resources in ${duration.toMillis() / 1000f}s")
		}
		
		try {
			rootConfig.mappingFile.whenNotNull { file ->
				if (mappings.isNotEmpty()) {
					PrintWriter(FileOutputStream(file)).use {
						for ((key, value) in mappings) {
							it.println(key.replace(",", "\\,") + "," + value.replace(",", "\\,"))
						}
					}
				}
			}
		} catch (t: Throwable) {
			Exception("Error writing mapping file", t).printStackTrace()
		}
	}
	
	fun noMethodInsns(methodNode: MethodNode) =
		Modifier.isAbstract(methodNode.access) || Modifier.isNative(methodNode.access)
	
	fun randomWeight(weight: Int): Boolean {
		return random.nextInt(weight) == 0
	}

	@Suppress("NOTHING_TO_INLINE", "ConstantConditionIf")
	inline fun debug(message: Any) {
		if (false) {
			println(message)
		}
	}
}
