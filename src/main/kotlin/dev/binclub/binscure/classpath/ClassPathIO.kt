package dev.binclub.binscure.classpath

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.kotlin.wrap
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
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
		//CObfuscator.getProgressBar("Loading Input").use { progressBar ->
			JarFile(file).use {
				//progressBar.maxHint(it.size().toLong())
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
					//progressBar.step()
				}
			}
		//}
	}
	
	fun loadClassPath(files: Collection<File>) {
		if (files.isEmpty())
			return
		
		for (file in CObfuscator.getProgressBar("Loading Libraries").wrap(files)) {
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
	
	fun writeOutput(file: File) {
		//CObfuscator.getProgressBar("Writing Output").use { progressBar ->
			//progressBar.maxHint((ClassPath.passThrough.size + ClassPath.classes.size).toLong())
			JarOutputStream(FileOutputStream(file)).use {
				val crc = DummyCRC(0xDEADBEEF)
				if (rootConfig.crasher.enabled) {
					val field = ZipOutputStream::class.java.getDeclaredField("crc")
					field.isAccessible = true
					field.set(it, crc)
				}
				
				for ((name, bytes) in ClassPath.passThrough) {
					crc.overwrite = false
					it.putNextEntry(ZipEntry(name))
					it.write(bytes)
					it.closeEntry()
					//progressBar.step()
				}
				for (classNode in ClassPath.classes.values) {
					if (!CObfuscator.isExcluded(classNode)) {
						crc.overwrite = true
						classNode.fields?.shuffle(CObfuscator.random)
						classNode.methods?.shuffle(CObfuscator.random)
						classNode.innerClasses?.shuffle(CObfuscator.random)
					}
					
					var name = "${classNode.name}.class"
					if (!CObfuscator.isExcluded(classNode) && rootConfig.crasher.enabled) {
						name += "/"
						crc.overwrite = true
					}
					
					val entry = ZipEntry(name)
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
					it.write(writer.toByteArray())
					it.closeEntry()
					
					crc.overwrite = false
					//progressBar.step()
				}
			}
		//}
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
}