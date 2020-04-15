package dev.binclub.binscure.classpath.tree

/**
 * @author cookiedragon234 25/Jan/2020
 */
abstract class ClassTreeEntry {
	abstract fun getName(): String
	abstract fun getDirectSuper(): String?
	abstract fun getSuperClasses(): Set<String>
	abstract fun getAccess(): Int
	abstract fun getMethods(): Set<MethodInfo>
	abstract fun getFields(): Set<FieldInfo>
}

data class FieldInfo(val owner: String, val name: String, val description: String)
data class MethodInfo(val owner: String, val name: String, val desc: String)
