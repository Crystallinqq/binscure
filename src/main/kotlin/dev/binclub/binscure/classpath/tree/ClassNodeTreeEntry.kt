package dev.binclub.binscure.classpath.tree

import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 25/Jan/2020
 */
class ClassNodeTreeEntry(val classNode: ClassNode): ClassTreeEntry() {
	override fun getAccess(): Int = classNode.access
	
	override fun getDirectSuper(): String? {
		return classNode.superName
	}
	
	private val supers: Set<String> by lazy {
		val set: MutableSet<String> = if (classNode.superName != null) {
			mutableSetOf(classNode.superName)
		} else {
			mutableSetOf()
		}
		set.addAll(classNode.interfaces)
		set
	}
	private val methods0: Set<MethodInfo> by lazy {
		val set = mutableSetOf<MethodInfo>()
		for (method in classNode.methods) {
			set.add(MethodInfo(classNode.name, method.name, method.desc))
		}
		set
	}
	private val fields0: Set<FieldInfo> by lazy {
		val set = mutableSetOf<FieldInfo>()
		for (field in classNode.fields) {
			set.add(FieldInfo(classNode.name, field.name, field.desc))
		}
		set
	}
	
	override fun getName(): String = classNode.name
	override fun getSuperClasses(): Set<String> = supers
	override fun getMethods(): Set<MethodInfo> = methods0
	override fun getFields(): Set<FieldInfo> = fields0
}
