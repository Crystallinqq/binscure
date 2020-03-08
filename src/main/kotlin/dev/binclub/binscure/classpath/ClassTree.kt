package dev.binclub.binscure.classpath

import dev.binclub.binscure.classpath.tree.ClassTreeEntry

/**
 * @author cookiedragon234 23/Jan/2020
 */
class ClassTree(val thisClass: ClassTreeEntry) {
	val parents = mutableSetOf<ClassTreeEntry>()
	val children = mutableSetOf<ClassTreeEntry>()
}
