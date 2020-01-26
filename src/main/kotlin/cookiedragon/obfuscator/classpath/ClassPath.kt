package cookiedragon.obfuscator.classpath

import cookiedragon.obfuscator.classpath.tree.ClassNodeTreeEntry
import cookiedragon.obfuscator.classpath.tree.ClassPathTreeEntry
import cookiedragon.obfuscator.classpath.tree.ClassTreeEntry
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 23/Jan/2020
 */
object ClassPath {
	val classes = mutableMapOf<String, ClassNode>()
	val classPath = mutableMapOf<String, ClassNode>()
	val passThrough = mutableMapOf<String, ByteArray>()
	private val treeEntries = mutableMapOf<String, ClassTreeEntry>()
	val hierachy = mutableMapOf<String, ClassTree>()
	val originalNames = mutableMapOf<ClassNode, String>()
	
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
				} catch (ignored: Throwable){return null}
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
	
	
	operator fun get(className: String) = classPath[className]
	operator fun set(className: String, classNode: ClassNode) {
		classPath[className] = classNode
	}
}
