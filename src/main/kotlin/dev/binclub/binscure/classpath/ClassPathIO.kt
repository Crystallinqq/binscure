package dev.binclub.binscure.classpath

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.CObfuscator.random
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.replaceLast
import dev.binclub.binscure.utils.DummyHashSet
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
						ClassReader(bytes)
							.accept(classNode, ClassReader.EXPAND_FRAMES)
						
						if (!rootConfig.hardExclusions.any { entry.name.startsWith(it.trim()) }) {
							ClassPath.classes[classNode.name] = classNode
						} else {
							ClassPath.passThrough[entry.name] = bytes
						}
						ClassPath.classPath[classNode.name] = classNode
						ClassPath.originalNames[classNode] = classNode.name
					} else if (!entry.isDirectory) {
						ClassPath.passThrough[entry.name] = bytes
					}
				}
			}
		}
	}
	
	fun loadClassPath(files: Collection<File>) {
		if (files.isEmpty())
			return
		
		for (file in files) {
			if (file.isDirectory) {
				loadClassPath(file.listFiles()!!.asList())
			} else if (file.extension == "jar" || file.extension == "zip") {
				JarFile(file).use {
					for (entry in it.entries()) {
						if (!entry.isDirectory && entry.name.endsWith(".class")) {
							val classNode = ClassNode()
							ClassReader(it.getInputStream(entry).readBytes())
								.accept(classNode, ClassReader.EXPAND_FRAMES)
							ClassPath.classPath[classNode.name] = classNode
							ClassPath.originalNames[classNode] = classNode.name
						}
					}
				}
			}
		}
	}
	
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
			
			for ((name, bytes) in ClassPath.passThrough) {
				crc.overwrite = false
				it.putNextEntry(ZipEntry(name))
				it.write(bytes)
				it.closeEntry()
			}
			for (classNode in ClassPath.classes.values) {
				try {
					if (!CObfuscator.isExcluded(classNode)) {
						crc.overwrite = true
						classNode.fields?.shuffle(random)
						classNode.methods?.shuffle(random)
						classNode.innerClasses?.shuffle(random)
					}
					
					var name = "${classNode.name}.class"
					if (!CObfuscator.isExcluded(classNode) && rootConfig.crasher.enabled && rootConfig.crasher.checksums) {
						crc.overwrite = true
						
						it.putNextEntry(ZipEntry(name.replaceLast('/', "/\u0000")))
						it.write(0x00)
					}
					
					val entry = ZipEntry(name)
					
					if (!CObfuscator.isExcluded(classNode) && rootConfig.crasher.enabled && rootConfig.crasher.checksums) {
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
