package dev.binclub.binscure.classpath

import dev.binclub.binscure.classpath.tree.ClassTreeEntry
import java.util.*
import kotlin.collections.HashSet

/**
 * @author cookiedragon234 23/Jan/2020
 */
class ClassTree(val thisClass: ClassTreeEntry) {
	val parents = mutableSetOf<ClassTreeEntry>()
	val children = mutableSetOf<ClassTreeEntry>()
	
	val allParents: Set<String> by lazy {
		val out = HashSet<String>()
		val toProcess = arrayListOf<ClassTreeEntry>()
		toProcess.addAll(this.parents)
		while (toProcess.isNotEmpty()) {
			val parent = toProcess.removeLast()
			if (out.add(parent.getName())) {
				val tempTree = ClassPath.getHierarchy(parent.getName())
				if (tempTree != null) {
					toProcess.addAll(tempTree.parents)
				}
			}
		}
		out
	}
}
