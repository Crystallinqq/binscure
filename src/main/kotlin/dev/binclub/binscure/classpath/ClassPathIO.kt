package dev.binclub.binscure.classpath

import dev.binclub.binscure.CObfuscator.random
import dev.binclub.binscure.classpath.ClassPath.classes
import dev.binclub.binscure.classpath.ClassPath.passThrough
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.replaceLast
import dev.binclub.binscure.utils.DummyHashSet
import dev.binclub.binscure.utils.isExcluded
import dev.binclub.binscure.utils.versionAtLeast
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


/**
 * @author cookiedragon234 25/Jan/2020
 */
object ClassPathIO {
	fun loadInputJar(file: File) {
		if (rootConfig.useJavaClassloader) {
			addFileToClassPath(file)
		} else {
			JarFile(file).use {
				for (entry in it.entries()) {
					val bytes = it.getInputStream(entry).readBytes()
					if (!entry.isDirectory && entry.name.endsWith(".class") && !entry.name.endsWith("module-info.class")) {
						val classNode = ClassNode()
						
						try {
							ClassReader(bytes)
								.accept(classNode, 0)
						} catch (t: Throwable) {
							System.err.println("Error reading class file, skipping")
							t.printStackTrace(System.err)
							continue
						}
						
						val excluded = rootConfig.tExclusions.isExcluded(classNode)
						val hardExcluded = rootConfig.hardExclusions.any { entry.name.startsWith(it.trim()) }
						
						if (!classNode.versionAtLeast(Opcodes.V1_7) && !excluded && !hardExcluded) {
							println("Unsupported <J8 class file ${entry.name}, will not be obfuscated as severely")
						}
						
						if (!excluded && !hardExcluded) {
							if (rootConfig.shuffleFields) {
								classNode.fields?.shuffle(random)
							}
							if (rootConfig.shuffleMethods) {
								classNode.methods?.shuffle(random)
							}
							if (rootConfig.shuffleClasses) {
								classNode.innerClasses?.shuffle(random)
							}
						}
						
						if (!hardExcluded) {
							classes[classNode.name] = classNode
						} else {
							passThrough[entry.name] = bytes
						}
						ClassPath.classPath[classNode.name] = classNode
						ClassPath.originalNames[classNode] = classNode.name
					} else if (!entry.isDirectory) {
						passThrough[entry.name] = bytes
					}
				}
			}
		}
	}
	
	fun loadClassPath(files: Collection<File>) = ClassPath.loadClassPath(files.toTypedArray())
	
	val emptyClass: ByteArray by lazy {
		ClassWriter(0).also {
			ClassNode().apply {
				version = Opcodes.V1_8
				name = ""
			}.accept(it)
		}.toByteArray()
	}
	
	fun writeOutput(file: File) {
		val fileOut = FileOutputStream(file)
		JarOutputStream(fileOut).use {
			namesField[it] = DummyHashSet<String>()
			val crc = DummyCRC(0xDEADBEEF)
			if (rootConfig.crasher.enabled && rootConfig.crasher.checksums) {
				crcField[it] = crc
				it.putNextEntry(ZipEntry("â\u3B25\u00d4\ud400®©¯\u00EB\u00A9\u00AE\u008D\u00AA\u002E"))
			}
			
			for ((i, entry) in passThrough.entries.withIndex()) {
				if (rootConfig.printProgress) {
					print(rootConfig.getLineChar())
					val percentStr = ((i.toFloat() / passThrough.size) * 100).toInt().toString().padStart(3, ' ')
					print("Writing resources ($percentStr% - $i/${passThrough.size})".padEnd(100, ' '))
				}
				crc.overwrite = false
				it.putNextEntry(ZipEntry(entry.key))
				it.write(entry.value)
				it.closeEntry()
			}
			
			if (rootConfig.printProgress) {
				print(rootConfig.getLineChar())
			}
			
			for ((i, classNode) in classes.values.withIndex()) {
				val excluded = rootConfig.tExclusions.isExcluded(classNode)
				if (rootConfig.printProgress) {
					print(rootConfig.getLineChar())
					val percentStr = ((i.toFloat() / classes.size) * 100).toInt().toString().padStart(3, ' ')
					print("Writing classes ($percentStr% - $i/${classes.size})".padEnd(100, ' '))
				}
				try {
					if (!excluded) {
						crc.overwrite = true
						if (rootConfig.shuffleFields) {
							classNode.fields?.shuffle(random)
						}
						if (rootConfig.shuffleMethods) {
							classNode.methods?.shuffle(random)
						}
						if (rootConfig.shuffleClasses) {
							classNode.innerClasses?.shuffle(random)
						}
					}
					
					val name = "${classNode.name}.class"
					if (!excluded && rootConfig.crasher.enabled && rootConfig.crasher.checksums) {
						crc.overwrite = true
						
						it.putNextEntry(ZipEntry(name.replaceLast('/', "/\u0000")))
						it.write(0x50)
						it.write(0x4B)
						it.write(0x03)
						it.write(0x04)
					}
					
					val entry = ZipEntry(name)
					
					if (!excluded && rootConfig.crasher.enabled && rootConfig.crasher.checksums) {
						it.putNextEntry(ZipEntry(entry.name))
					}
					
					it.putNextEntry(entry)
					
					var writer: ClassWriter
					try {
						writer = CustomClassWriter(ClassWriter.COMPUTE_FRAMES)
						classNode.accept(writer)
					} catch (e: Throwable) {
						println("Error while writing class ${classNode.name}")
						e.printStackTrace()
						
						writer = CustomClassWriter(0)
						classNode.accept(writer)
					}
					val arr = writer.toByteArray()
					it.write(arr)
					it.closeEntry()
					
					crc.overwrite = false
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}
			
			if (rootConfig.printProgress) {
				print(rootConfig.getLineChar())
			}
		}
	}
	
	val namesField: Field by lazy {
		ZipOutputStream::class.java.getDeclaredField("names").also {
			it.isAccessible = true
		}
	}
	
	val crcField: Field by lazy {
		ZipOutputStream::class.java.getDeclaredField("crc").also {
			it.isAccessible = true
		}
	}
	
	val timeField: Field by lazy {
		ZipEntry::class.java.getDeclaredField("csize").also {
			it.isAccessible = true
		}
	}
	
	val commentField: Field by lazy {
		ZipOutputStream::class.java.getDeclaredField("comment").also {
			it.isAccessible = true
		}
	}
	
	private class DummyCRC(val crc: Long): CRC32() {
		var overwrite = false
		
		override fun getValue(): Long {
			return if (overwrite) {
				crc
			} else {
				super.getValue()
			}
		}
	}
	
	private fun addFileToClassPath(file: File) =
		URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).let {
			it.isAccessible = true
			it(ClassLoader.getSystemClassLoader(), file.toURI().toURL())
		}
}
