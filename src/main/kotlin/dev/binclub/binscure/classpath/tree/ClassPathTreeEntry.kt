package dev.binclub.binscure.classpath.tree

import org.objectweb.asm.Type

/**
 * @author cookiedragon234 25/Jan/2020
 */
class ClassPathTreeEntry(val clazz: Class<*>): ClassTreeEntry() {
	override fun getAccess(): Int = clazz.modifiers
	
	override fun getDirectSuper(): String? {
		return if (clazz.superclass != null) {
			Type.getInternalName(clazz.superclass)
		} else {
			"java/lang/Object"
		}
	}
	
	private val supers: Set<String> by lazy {
		val set: MutableSet<String> = if (clazz.superclass != null) {
			mutableSetOf(Type.getInternalName(clazz.superclass))
		} else {
			mutableSetOf()
		}
		for (aInterface in clazz.interfaces) {
			set.add(Type.getInternalName(aInterface))
		}
		set
	}
	private val methods0: Set<MethodInfo> by lazy {
		val set = mutableSetOf<MethodInfo>()
		for (declaredMethod in clazz.declaredMethods) {
			set.add(MethodInfo(getName(), declaredMethod.name, Type.getMethodDescriptor(declaredMethod)))
		}
		set
	}
	private val fields0: Set<FieldInfo> by lazy {
		val set = mutableSetOf<FieldInfo>()
		for (field in clazz.declaredFields) {
			set.add(FieldInfo(getName(), field.name, Type.getDescriptor(field.type)))
		}
		set
	}
	
	override fun getName(): String = Type.getInternalName(clazz)
	override fun getSuperClasses(): Set<String> = supers
	override fun getMethods(): Set<MethodInfo> = methods0
	override fun getFields(): Set<FieldInfo> = fields0
}
