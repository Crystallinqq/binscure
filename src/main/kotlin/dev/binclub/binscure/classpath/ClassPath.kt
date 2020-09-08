package dev.binclub.binscure.classpath

import dev.binclub.binscure.classpath.tree.ClassNodeTreeEntry
import dev.binclub.binscure.classpath.tree.ClassPathTreeEntry
import dev.binclub.binscure.classpath.tree.ClassTreeEntry
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.block
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile

/**
 * @author cookiedragon234 23/Jan/2020
 */
object ClassPath {
	val classes = mutableMapOf<String, ClassNode>()
	val classPath = mutableMapOf<String, ClassNode>()
	val passThrough = mutableMapOf<String, ByteArray>()
	private val treeEntries = hashMapOf<String, ClassTreeEntry>()
	val hierachy = hashMapOf<String, ClassTree>()
	val originalNames = hashMapOf<ClassNode, String>()
	
	
	private val warnings = mutableSetOf<String>()
	
	fun warn(type: String) = warn(type, Unit)
	
	fun <T> warn(type: String, out: T): T {
		if (!rootConfig.ignoreClassPathNotFound && type != "give up" && type != "java/lang/YourMum" && warnings.add(type)) {
			System.err.println("\rWARNING: $type was not found in the classpath, may cause sideaffects")
		}
		return out
	}
	
	fun getHierarchy(name: String): ClassTree? {
		if (hierachy.containsKey(name))
			return hierachy[name]!!
		
		if (treeEntries.containsKey(name)) {
			val tree = treeEntries[name]!!
			constructTreeSuperClasses(tree)
			constructTreeHiearchy(name, tree)
		} else {
			if (classPath.containsKey(name)) {
				val tree = ClassNodeTreeEntry(classPath[name]!!)
				treeEntries[name] = tree
				constructTreeSuperClasses(tree)
				constructTreeHiearchy(name, tree)
			} else {
				try {
					val tree = ClassPathTreeEntry(Class.forName(name.replace('/', '.')))
					treeEntries[name] = tree
					constructTreeSuperClasses(tree)
					constructTreeHiearchy(name, tree)
				} catch (ignored: Throwable){
					return warn(name, null)
				}
			}
		}
		
		return hierachy[name]!!
	}
	
	fun constructTreeSuperClasses(treeEntry: ClassTreeEntry) {
		for (aSuper in treeEntry.getSuperClasses()) {
			if (!classPath.containsKey(aSuper)) {
				try {
					val clazz = Class.forName(aSuper.replace('/', '.'))
					treeEntries[aSuper] = ClassPathTreeEntry(clazz)
				} catch (ignored: Throwable){}
			}
		}
	}
	
	fun constructHierarchy() {
		//val start = Instant.now()
		
		treeEntries.clear()
		hierachy.clear()
		for (classNode in classPath.values) {
			val entry = ClassNodeTreeEntry(classNode)
			treeEntries[classNode.name] = entry
			constructTreeSuperClasses(entry)
		}
		
		for ((name, entry) in treeEntries) {
			constructTreeHiearchy(name, entry)
		}
		
		//println("Finished Constructing Hierarchy in ${Duration.between(start, Instant.now()).toMillis() * 1000}s (${hierachy.size} | ${treeEntries.size} entries)")
	}
	
	private fun constructTreeHiearchy(name: String, entry: ClassTreeEntry) {
		val tree = ClassTree(entry)
		hierachy[name] = tree
		for (aSuper in entry.getSuperClasses()) {
			val superTree = treeEntries[aSuper]
			if (superTree != null) {
				tree.parents.add(superTree)
			}
		}
		for (entry2 in treeEntries.values) {
			if (entry2.getSuperClasses().contains(name)) {
				tree.children.add(entry2)
			}
		}
	}
	
	
	
	
	private val librarySources = arrayListOf<LibrarySource>()
	
	fun findClassNode(className: String): ClassNode? {
		if (rootConfig.lazyLibraryLoading) {
			return classPath[className] ?: findClassInLibraries(className)
		} else {
			return classPath[className]
		}
	}
	
	private fun findClassInLibraries(className: String): ClassNode? {
		librarySources.forEach {
			val found = it.findClass(className)
			if (found != null) {
				return found
			}
		}
		
		return null
	}
	
	fun loadClassPath(files: Array<File>) {
		for (file in files) {
			if (file.isDirectory) {
				loadClassPath(file.listFiles()!!)
			} else if (file.extension == "jar" || file.extension == "zip") {
				if (rootConfig.lazyLibraryLoading) {
					librarySources.add(ZipLibrarySource(file))
				} else {
					JarFile(file).use {
						for (entry in it.entries()) {
							if (!entry.isDirectory && entry.name.endsWith(".class")) {
								val classNode = ClassNode()
								ClassReader(it.getInputStream(entry).readBytes())
									.accept(classNode, 0)
								classPath[classNode.name] = classNode
							}
						}
					}
				}
			} else {
				println("Unrecognised library type ${file.extension}")
			}
		}
	}
	
	abstract class LibrarySource {
		abstract fun findClass(className: String): ClassNode?
	}
	
	class ZipLibrarySource(val file: File): LibrarySource() {
		private val jar = JarFile(file)
		
		override fun findClass(className: String): ClassNode? {
			val resourceName = "$className.class"
			for (entry in jar.entries()) {
				if (entry.name.removeSuffix("/") == resourceName) {
					val classNode = ClassNode()
					ClassReader(jar.getInputStream(entry).readBytes())
						.accept(classNode, 0)
					classPath[classNode.name] = classNode
					return classNode
				}
			}
			return null
		}
		
		override fun toString(): String {
			return file.path
		}
	}
}
