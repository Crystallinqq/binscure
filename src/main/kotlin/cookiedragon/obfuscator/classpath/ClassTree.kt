package cookiedragon.obfuscator.classpath

import cookiedragon.obfuscator.classpath.tree.ClassTreeEntry

/**
 * @author cookiedragon234 23/Jan/2020
 */
class ClassTree(val thisClass: ClassTreeEntry) {
	val parents = mutableSetOf<ClassTreeEntry>()
	val children = mutableSetOf<ClassTreeEntry>()
}
